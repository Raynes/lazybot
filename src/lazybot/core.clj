(ns lazybot.core
  (:use [lazybot registry info]
	[clojure.stacktrace :only [root-cause]]
        [somnium.congomongo :only [mongo!]]
        [clojure.set :only [intersection]]
        [compojure.core :only [routes]]
        ring.middleware.params)
  (:import [java.io File FileReader]))

(def bots (atom {}))

(defn initiate-mongo []
  (try
    (mongo! :db (or (:db (read-config)) "lazybot"))
    (catch Throwable e
      (println "Error starting mongo (see below), carrying on without it")
      (.printStackTrace e *out*))))

(defn call-all [{bot :bot :as ircm} hook-key]
  (doseq [hook (pull-hooks bot hook-key)]
    (hook ircm)))

(def initial-hooks
     {:on-message [{:fn (fn [irc-map] (try-handle irc-map))}]
      :on-quit []
      :on-join []})

(defn reload-config [bot]
  (alter bot assoc :config (read-config)))

(defn load-plugin [irc refzors plugin]
  (let [ns (symbol (str "lazybot.plugins." plugin))]
    (require ns :reload)
    ((resolve (symbol (str ns "/load-this-plugin"))) irc refzors)))

(defn safe-load-plugin [irc refzors plugin]
  (try
    (load-plugin irc refzors plugin)
    true
    (catch Exception e false)))
 
(defn load-plugins [irc refzors]
  (let [info (:config @refzors)]
    (doseq [plug (:plugins (info (:server @irc)))]
      (load-plugin irc refzors plug))))

(defn reload-configs
  "Reloads the bot's configs. Must be run in a transaction."
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
  (alter-var-root #'lazybot.core/sroutes (constantly (apply routes rs))))

(defn reload-all
  "A clever function to reload everything when running lazybot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the nature
  of plugins. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  [& bots]
  (require 'lazybot.registry :reload)
  (require 'lazybot.utilities :reload)
  (route (extract-routes bots))
  (doseq [{:keys [com bot]} bots]
    (doseq [{:keys [cleanup]} (vals (:modules @bot))]
      (when cleanup (cleanup)))
    (dosync
     (alter bot dissoc :modules)
     (alter bot assoc-in [:modules :internal :hooks] initial-hooks)
     (reload-config bot))
    (load-plugins com bot)))