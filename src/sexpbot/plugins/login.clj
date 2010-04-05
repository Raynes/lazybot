(ns sexpbot.plugins.login
  (:use [sexpbot commands respond info privileges]))

(defn check-pass [user pass]
  (let [pass (((read-config) :admins) user)
	privs (->> user ((read-config)) (with-info (str sexpdir "/privs.clj")))]
    (when (seq pass) (dosync (alter logged-in assoc user privs)))))

(defn logged-in? [user] (some #{user} (keys @logged-in)))

(defmethod respond :login [{:keys [bot sender channel args]}]
  (if (check-pass sender (first args))
    (.sendMessage bot channel "You've been logged in.")
    (.sendMessage bot channel "Username and password combination do not match.")))

(defmethod respond :logout [{:keys [bot sender channel]}]
  (dosync (alter logged-in dissoc sender)
	  (.sendMessage bot channel "You've been logged out.")))

(defmethod respond :quit [{:keys [sender]}]
  (when (logged-in? sender) (dosync (alter logged-in dissoc sender))))

(defmodule :login
  {"login"  :login
   "logout" :logout
   "quit"   :quit})