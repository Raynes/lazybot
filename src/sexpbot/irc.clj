(ns sexpbot.irc
  (:use [sexpbot core info]
        [clj-config.core :only [read-config]])
  (:require [irclj.core :as ircb]))

(defn make-callbacks []
  (let [refzors (ref {:protocol :irc :modules {:internal {:hooks initial-hooks}} :config initial-info :pending-ops 0})]
    [(into {}
           (map
            (fn [key]
              [key
               (fn [irc-map]
                 (call-all (dissoc (assoc irc-map :bot refzors :com (:irc irc-map)) :irc)
                           key))])
            [:on-any :on-message :on-quit :on-join]))
     refzors]))

(defn make-bot-run [name pass server fnmap]
  (ircb/create-irc {:name name :password pass :server server :fnmap fnmap}))

(defn make-bot [server] 
  (let [bot-config (eval (read-config info-file))
	name (:bot-name (bot-config server))
	pass (:bot-password (bot-config server))
	channels (:channels (bot-config server))
        [fnmap refzors] (make-callbacks)
	irc (ircb/connect (make-bot-run name pass server fnmap) :channels channels :identify-after-secs 3)]
    [irc refzors]))