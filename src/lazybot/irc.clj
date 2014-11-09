(ns lazybot.irc
  (:require [lazybot.core :as lazybot]
            [lazybot.info :as info]
            [useful.fn :refer [decorate]]
            [useful.map :refer [keyed]]
            [irclj.core :as ircb]
            [irclj.events :as events]))

(defn make-hook
  [actions]
  (fn [& args]
    (doseq [action @actions]
      (apply action args))))

(defn base-maps
  "Create the base callback and bot maps."
  [config]
  (let [refzors (ref {:modules {:internal {:hooks lazybot/initial-hooks}}
                      :config config
                      :pending-ops 0})]
    [(into {:raw-log events/stdout-callback}
           (map
            (decorate
             #(fn dispatch-hooks [irc-map event]
                (dosync (alter irc-map assoc :server (:network @irc-map)))
                (lazybot/call-all (-> @irc-map
                                      (assoc :bot refzors :com irc-map)
                                      (dissoc :irc)
                                      (assoc :event event)
                                      (assoc :bot-nick (:nick @irc-map))
                                      (assoc :nick (:nick event))
                                      (assoc :message (:text event))
                                      (assoc :channel ((:params event) 0)))
                                  %)))
            [:001 :privmsg :quit :join]))
     refzors]))

(defn make-bot
  "Creates a new bot and connects it."
  [server]
  (let [bot-config (info/read-config)
        port 6667
        [name pass channels] ((juxt :bot-name :bot-password :channels)
                                   (bot-config server))
        [fnmap refzors] (base-maps bot-config)
        irc (ircb/connect server port name
                          :callbacks fnmap
                          :identify-after-secs 3)]
    (ircb/identify irc pass)
    [irc refzors]))

(defn init-bot
  "Initialize a new bot."
  [server]
  (let [[irc refzors] (make-bot server)]
    (swap! lazybot/bots assoc server {:com irc :bot refzors})
    (dosync (lazybot/reload-config refzors))
    (lazybot/load-plugins irc refzors)))

(defn start-bots
  "Starts bots for servers."
  [servers]
  (doseq [serv servers]
    (init-bot serv))
  (lazybot/route (lazybot/extract-routes (vals @lazybot/bots))))
