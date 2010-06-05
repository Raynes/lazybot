(ns sexpbot.core
  (:use [sexpbot respond info]
	[clj-config.core :only [read-config]]
	[clojure.stacktrace :only [root-cause]])
  (:require [org.danlarkin.json :as json]
	    [irclj.irclj :as ircb])
  (:import [java.io File FileReader]))

(def info (read-config info-file))
(def prepend (:prepend info))

(defn call-all [{irc-map :irc :as ircm} hook-key]
  (doseq [hook (hook-key (apply merge-with concat (vals (:hooks @irc-map))))] (hook ircm)))

(defn make-fnmap []
  {:on-any (fn [irc-map] (call-all irc-map :on-any))
   :on-message (fn [irc-map] (call-all irc-map :on-message))
   :on-quit (fn [irc-map] (call-all irc-map :on-quit))
   :on-join (fn [irc-map] (call-all irc-map :on-join))})

(defn make-bot-run [name pass server]
  (ircb/create-irc {:name name :password pass :server server :fnmap (make-fnmap)}))

(defn make-bot [server] 
  (let [bot-config info
	name ((bot-config :bot-name) server)
	pass ((bot-config :bot-password) server)
	channels ((bot-config :channels) server)
	irc (ircb/connect (make-bot-run name pass server) :channels channels :identify-after-secs 3)]
    (dosync (alter irc assoc-in [:hooks] create-initial-hooks)
	    (alter irc assoc-in [:commands] initial-commands))
    irc))