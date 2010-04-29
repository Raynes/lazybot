(ns sexpbot.core
  (:use [sexpbot respond info utilities]
	[clojure.stacktrace :only [root-cause]])
  (:require [org.danlarkin.json :as json]
	    [irclj.irclj :as ircb])
  (:import [java.io File FileReader]))

(def info (read-config))
(def prepend (:prepend info))

(defn call-all [irc-map hooks hook-key] (doseq [hook (hook-key hooks)] (hook irc-map)))

(defn make-fnmap [] 
  {:on-message (fn [irc-map] (call-all irc-map @hooks :on-message))
   :on-quit (fn [irc-map] (call-all irc-map @hooks :on-quit))
   :on-join (fn [irc-map] (call-all irc-map @hooks :on-join))})

(defn make-bot-run [name pass server]
  (ircb/create-irc {:name name :password pass :server server :fnmap (make-fnmap)}))

(defn make-bot [server] 
  (let [bot-config (read-config)
	name ((bot-config :bot-name) server)
	pass ((bot-config :bot-password) server)
	channels ((bot-config :channels) server)
	irc (ircb/connect (make-bot-run name pass server) :channels channels :identify-after-secs 3)]
    irc))

(defn reload-all!
  "A clever function to reload everything when running sexpbot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the
  way refs are used. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  []
  (reset-hooks)
  (reset-commands)
  (reset-ref logged-in)
  (reset-ref modules)
  (use 'sexpbot.respond :reload)
  (reload-plugins)
  (doseq [plug (:plugins (read-config))] (.start (Thread. (fn [] (loadmod plug))))))