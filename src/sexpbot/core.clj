(ns sexpbot.core
  (:use [sexpbot registry info]
	[clj-config.core :only [read-config]]
	[clojure.stacktrace :only [root-cause]]
        [somnium.congomongo :only [mongo!]]
        [clojure.set :only [intersection]]
        [compojure.core :only [routes]]
        ring.middleware.params)
  (:import [java.io File FileReader]))

(def bots (atom {}))

(defonce initial-info (eval (read-config info-file)))

(mongo! :db (or (:db initial-info) "sexpbot"))

(defn call-all [{bot :bot :as ircm} hook-key]
  (doseq [hook (pull-hooks bot hook-key)] (hook ircm)))

(def initial-hooks
     {:on-message [{:fn (fn [irc-map] (try-handle irc-map))}]
      :on-quit []
      :on-join []})

(def all-plugins (:plugins (eval (read-config info-file))))

(def servers-port (:servers-port (eval (read-config info-file))))

(defn reload-config [bot]
  (alter bot assoc :config (eval (read-config info-file))))

(defn require-plugin [plugin]
  (require (symbol (str "sexpbot.plugins." plugin)) :reload))

(defn load-plugin [irc refzors plugin]
  ((resolve (symbol (str "sexpbot.plugins." plugin "/load-this-plugin"))) irc refzors))

(defn require-plugins []
  (doseq [plug ((eval (read-config info-file)) :plugins)]
    (require-plugin plug)))

(defn safe-load-plugin [irc refzors plugin]
  (try
    (load-plugin irc refzors plugin)
    true
    (catch Exception e false)))
 
(defn load-plugins [irc refzors]
  (let [info (:config @refzors)
	plugins-to-load (intersection (:plugins info) (:plugins (info (:server @irc))))]
    (doseq [plug plugins-to-load]
      (load-plugin irc refzors plug))))

(defn reload-configs
  "Reloads the bot's configs. Must be ran in a transaction."
  [& bots]
  (doseq [[_ bot] bots]
    (reload-config bot)))

(defn connect-bot [cfn server]
  (let [[irc refzors] (cfn server)]
    (swap! bots assoc server {:com irc :bot refzors})
    (dosync (reload-config refzors))
    (load-plugins irc refzors)))

(defn extract-routes [bots]
  (filter identity (apply concat (map #(->> % :bot deref :modules vals (map :routes)) bots))))

(def sroutes nil)

(defn route [rs]
  (alter-var-root #'sexpbot.core/sroutes (constantly (apply routes rs))))

(defn reload-all
  "A clever function to reload everything when running sexpbot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the nature
  of plugins. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  [& bots]
  (require 'sexpbot.registry :reload)
  (require 'sexpbot.utilities :reload)
  (require 'sexpbot.twitter :reload)
  (require-plugins)
  (route (extract-routes bots))
  (doseq [{:keys [com bot]} bots]
    (doseq [{:keys [cleanup]} (vals (:modules @bot))]
      (when cleanup (cleanup)))
    (dosync
     (alter bot dissoc :modules)
     (alter bot assoc-in [:modules :internal :hooks] initial-hooks)
     (reload-config bot))
    (load-plugins com bot)))