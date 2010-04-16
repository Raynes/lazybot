(ns sexpbot.plugins.login
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(defn check-pass [user pass]
  (let [userconf (((read-config) :users) user)]
    (when (= pass (userconf :pass)) 
      (dosync (alter logged-in assoc user (userconf :privs))))))

(defn logged-in? [user] (some #{user} (keys @logged-in)))

(defmethod respond :login [{:keys [bot sender channel args]}]
  (if (check-pass sender (first args))
    (ircb/send-message bot channel "You've been logged in.")
    (ircb/send-message bot channel "Username and password combination do not match.")))

(defmethod respond :logout [{:keys [bot sender channel]}]
  (dosync (alter logged-in dissoc sender)
	  (ircb/send-message bot channel "You've been logged out.")))

(defmethod respond :quit [{:keys [sender]}]
  (when (logged-in? sender) (dosync (alter logged-in dissoc sender))))

(defplugin
  {"login"  :login
   "logout" :logout
   "quit"   :quit})