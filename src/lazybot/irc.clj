(ns lazybot.irc
  (:use lazybot.core
        lazybot.info
        [useful.fn :only [decorate]]
        [useful.map :only [keyed]])
  (:require [irclj.core :as ircb]
            [lazybot.core :as core]
            [lazybot.info :as info]))

(defn base-maps
  "Create the base callback and bot maps."
  [config]
  (let [refzors (ref {:modules {:internal {:hooks core/initial-hooks}}
                      :config config
                      :pending-ops 0})]
    [(into {}
           (map
            (decorate
             #(fn [irc-map]
                (core/call-all (-> irc-map
                                   (assoc :bot refzors :com (:irc irc-map))
                                   (dissoc :irc))
                               %)))
            [:on-any :on-message :on-quit :on-join]))
     refzors]))

(defn make-bot-run
  "Create an irclj param map to pass to connect."
  [name password server fnmap]
  (ircb/create-irc (keyed [name password server fnmap])))

(defn make-bot
  "Creates a new bot and connects it."
  [server]
  (let [bot-config (read-config)
        [name pass channels] ((juxt :bot-name :bot-password :channels)
                              (bot-config server))
        [fnmap refzors] (base-maps bot-config)
        irc (ircb/connect (make-bot-run name pass server fnmap)
                          :channels channels, :identify-after-secs 3)]
    [irc refzors]))

(defn init-bot
  "Initialize a new bot."
  [server]
  (let [[irc refzors] (make-bot server)]
    (swap! core/bots assoc server {:com irc :bot refzors})
    (dosync (core/reload-config refzors))
    (core/load-plugins irc refzors)))

(defn start-bots
  "Starts bots for servers."
  [servers]
  (doseq [serv servers]
    (init-bot serv))
  (core/route (core/extract-routes (vals @core/bots))))
