(ns sexpbot.plugins.login
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(defn check-pass [user pass]
  (let [userconf (((read-config) :users) user)]
    (when (= pass (userconf :pass)) 
      (dosync (alter logged-in assoc user (userconf :privs))))))

(defn logged-in? [user] (some #{user} (keys @logged-in)))

(defplugin
  (:add-hook :on-quit
	     (fn [irc-map] (try-handle (assoc irc-map :message (str (:prepend (read-config)) "quit")))))

  (:login 
   "Best executed via PM. Give it your password, and it will log you in."
   ["login"] 
   [{:keys [irc nick channel args]}]
   (if (check-pass nick (first args))
     (ircb/send-message irc channel "You've been logged in.")
     (ircb/send-message irc channel "Username and password combination do not match.")))
  
  (:logout 
   "Logs you out."
   ["logout"] 
   [{:keys [irc nick channel]}]
   (dosync (alter logged-in dissoc nick)
	   (ircb/send-message irc channel "You've been logged out.")))

  (:quit "" ["quit"] [{:keys [nick]}] (when (logged-in? nick) (dosync (alter logged-in dissoc nick)))))