(ns sexpbot.core
  (:use [sexpbot respond info [load :only [initial-hooks]]]
	[clj-config.core :only [read-config]]
	[clojure.stacktrace :only [root-cause]]
        [somnium.congomongo :only [mongo!]])
  (:require [org.danlarkin.json :as json]
	    [irclj.irclj :as ircb])
  (:import [java.io File FileReader]))

(mongo! :db "sexpbot")

(def bots (atom {}))

(def initial-info (read-config info-file))

(defn call-all [{bot :bot :as ircm} hook-key]
  (doseq [hook (pull-hooks bot hook-key)] (hook ircm)))

(defn make-callbacks []
  (let [refzors (ref {:hooks initial-hooks :commands {} :config initial-info :configs {}})]
    [(into {}
           (map (fn [key] [key (fn [irc-map] (call-all (assoc irc-map :bot refzors) key))])
                [:on-any :on-message :on-quit :on-join]))
     refzors]))

(defn make-bot-run [name pass server fnmap]
  (ircb/create-irc {:name name :password pass :server server :fnmap fnmap}))

(defn make-bot [server] 
  (let [bot-config (read-config info-file)
	name (:bot-name (bot-config server))
	pass (:bot-password (bot-config server))
	channels (:channels (bot-config server))
        [fnmap refzors] (make-callbacks)
	irc (ircb/connect (make-bot-run name pass server fnmap) :channels channels :identify-after-secs 3)]
    [irc refzors]))