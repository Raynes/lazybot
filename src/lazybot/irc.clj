(ns lazybot.irc
  (:use [lazybot core info]
        [useful.fn :only [decorate]]
        [useful.map :only [keyed]])
  (:require [irclj.core :as ircb]))

(defn make-callbacks [config]
  (let [refzors (ref {:modules {:internal {:hooks initial-hooks}}
                      :config config
                      :pending-ops 0})]
    [(into {}
           (map
            (decorate
             #(fn [irc-map]
                (call-all (dissoc (assoc irc-map :bot refzors :com (:irc irc-map)) :irc)
                          %)))
            [:on-any :on-message :on-quit :on-join]))
     refzors]))

(defn make-bot-run [name password server fnmap]
  (ircb/create-irc (keyed [name password server fnmap])))

(defn make-bot [server]
  (let [bot-config (read-config)
        [name pass channels] ((juxt :bot-name :bot-password :channels)
                              (bot-config server))
        [fnmap refzors] (make-callbacks bot-config)
        irc (ircb/connect (make-bot-run name pass server fnmap)
                          :channels channels, :identify-after-secs 3)]
    [irc refzors]))