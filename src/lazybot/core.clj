(ns lazybot.core
  (:require
   [lazybot.registry :as registry]
   [lazybot.info :as info]
   [clojure.stacktrace :refer [root-cause]]
   [somnium.congomongo :refer [mongo!]]
   [clojure.set :refer [intersection]]
   [compojure.core :refer [routes]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.adapter.jetty :refer [run-jetty]])
  (:import [java.io File FileReader]))

;; This is pretty much the only global mutable state in the entire bot, and it
;; is entirely necessary. We pass the bot refs to commands and hooks, but
;; sometimes it can't be helped and you need to get at the bots outside of
;; a defplugin. Very little code uses this atom.
(def bots
  "All of the bots that are currently running."
  (atom {}))

(defn initiate-mongo
  "Initiate the mongodb connection and set it globally."
  []
  (try
    (mongo! :db (or (:db (info/read-config)) "lazybot"))
    (catch Throwable e
      (println "Error starting mongo (see below), carrying on without it")
      (.printStackTrace e))))

(defn call-all
  "Call all hooks of a specific type."
  [{bot :bot :as ircm} hook-key]
  (when-not (registry/ignore-message? ircm)
    (doseq [hook (registry/pull-hooks bot hook-key)]
      (hook ircm))))

;; Note that even the actual handling of commands is done via a hook.
(def initial-hooks
  "The hooks that every bot, even without plugins, needs to have."
  {:privmsg [{:fn (fn [irc-map] (registry/try-handle irc-map))}]
   :on-quit []
   :on-join []})

(defn reload-config
  "Reloads and sets the configuration in a bot."
  [bot]
  (alter bot assoc :config (info/read-config)))

;; A plugin is just a file on the classpath with a namespace of
;; `lazybot.plugins.<x>` that contains a call to defplugin.
(defn load-plugin
  "Load a plugin (a Clojure source file)."
  [irc refzors plugin]
  (let [ns (symbol (str "lazybot.plugins." plugin))]
    (require ns :reload)
    ((resolve (symbol (str ns "/load-this-plugin"))) irc refzors)))

(defn safe-load-plugin
  "Load a plugin. Returns true if loading it was successful, false if
   otherwise."
  [irc refzors plugin]
  (try
    (load-plugin irc refzors plugin)
    true
    (catch Exception e false)))

(defn load-plugins
  "Load all plugins specified in the bot's configuration."
  [irc refzors]
  (let [info (:config @refzors)]
    (doseq [plug (:plugins (info (:server @irc)))]
      (load-plugin irc refzors plug))))

(defn reload-configs
  "Reloads the bot's configs. Must be ran in a transaction."
  [& bots]
  (doseq [[_ bot] bots]
    (reload-config bot)))

(defn extract-routes
  "Extracts the routes from bots."
  [bots]
  (->> bots
       (mapcat #(->> % :bot deref :modules vals (map :routes)))
       (filter identity)))

(def sroutes nil)

(defn route [rs]
  (alter-var-root #'lazybot.core/sroutes (constantly (wrap-params (apply routes rs)))))

(defn start-server [port]
  (defonce server (run-jetty #'lazybot.core/sroutes
                             {:port port :join? false})))

(defn reload-all
  "A clever function to reload everything when running lazybot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the nature
  of plugins. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  [& bots]
  (require 'lazybot.registry :reload)
  (require 'lazybot.utilities :reload)
  (require 'lazybot.paste :reload)
  (route (extract-routes bots))
  (doseq [{:keys [com bot]} bots]
    (doseq [{:keys [cleanup]} (vals (:modules @bot))]
      (when cleanup (cleanup)))
    (dosync
     (alter bot dissoc :modules)
     (alter bot assoc-in [:modules :internal :hooks] initial-hooks)
     (reload-config bot))
    (load-plugins com bot)))
