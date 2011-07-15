(ns lazybot.irc
  (:use [lazybot core info]
        [clj-config.core :only [read-config]]
        [amalloy.utils :only [decorate keywordize]])
  (:require [irclj.core :as ircb]))

(defn make-callbacks []
  (let [refzors (ref {:protocol :irc :modules {:internal {:hooks initial-hooks}} :config initial-info :pending-ops 0})]
    [(into {}
           (map
            (decorate
             #(fn [irc-map]
                (call-all (dissoc (assoc irc-map :bot refzors :com (:irc irc-map)) :irc)
                          %)))
            [:on-any :on-message :on-quit :on-join]))
     refzors]))

(defn make-bot-run [name password server fnmap]
  (ircb/create-irc (keywordize [name password server fnmap])))

(defn make-bot [server] 
  (let [bot-config (eval (read-config info-file))
	[name pass channels] ((juxt :bot-name :bot-password :channels)
                              (bot-config server))
        [fnmap refzors] (make-callbacks)
	irc (ircb/connect (make-bot-run name pass server fnmap)
                          :channels channels, :identify-after-secs 3)]
    [irc refzors]))