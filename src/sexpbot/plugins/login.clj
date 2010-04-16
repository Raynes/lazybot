(ns sexpbot.plugins.login
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(defn check-pass [user pass]
  (let [userconf (((read-config) :users) user)]
    (when (= pass (userconf :pass)) 
      (dosync (alter logged-in assoc user (userconf :privs))))))

(defn logged-in? [user] (some #{user} (keys @logged-in)))

(defmethod respond :login [{:keys [irc nick channel args]}]
  (if (check-pass nick (first args))
    (ircb/send-message irc channel "You've been logged in.")
    (ircb/send-message irc channel "Username and password combination do not match.")))

(defmethod respond :logout [{:keys [irc nick channel]}]
  (dosync (alter logged-in dissoc nick)
	  (ircb/send-message irc channel "You've been logged out.")))

(defmethod respond :quit [{:keys [nick]}]
  (when (logged-in? nick) (dosync (alter logged-in dissoc nick))))

(defplugin
  {"login"  :login
   "logout" :logout
   "quit"   :quit})