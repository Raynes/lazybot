(ns sexpbot.plugins.help
  (:use [sexpbot respond info gist]
	stupiddb.core)
  (:require [irclj.irclj :as ircb]))

(def info (read-config))
(def admin-add? (:admin-add? info))
(def admin-rm? (:admin-rm? info))

(def help-file (str (System/getProperty "user.home") "/.sexpbot/help.db"))
(def db (db-init help-file 30))


(defmethod respond :addtopic [{:keys [irc nick channel args]}]
  (let [topic (first args)
	content (->> args
		   (interpose " ")
		   (rest)
		   (apply str))]
    (cond
     (not-empty (db-get db topic)) (ircb/send-message irc channel "Topic already exists!")
     (or (empty? (.trim topic))
	 (empty? (.trim content))) (ircb/send-message irc channel "Neither topic nor content can be empty!")
      :else  (if admin-add?
	       (if (= :admin (get-priv nick))
		 (do
		   (db-assoc db topic content)
		   (ircb/send-message irc channel (str "Topic Added: " topic)))
		 (ircb/send-message irc channel (str nick ": Only admins can add topics!")))
	       (do
		 (db-assoc db topic content)
		 (ircb/send-message irc channel (str "Topic Added: " topic)))))))

(defmethod respond :rmtopic [{:keys [irc nick channel args]}]
  (let [topic (first args)]
     (if (not-empty (db-get db topic))
       (if admin-rm?
	 (if (= :admin (get-priv nick))
	   (do
	     (db-dissoc db topic)
	     (ircb/send-message irc channel (str "Topic Removed: " topic)))
	   (ircb/send-message irc channel (str nick ": Only admins can remove topics!"))))
       (ircb/send-message irc channel (str "Topic: \"" topic  "\" doesn't exist!")))))

(defmethod respond :help [{:keys [irc nick channel args]}]
  (let [topic (first args)
	content (db-get db topic)]
    (if (not-empty content)
      (do (ircb/send-message irc channel (str topic ":"))
	  (ircb/send-message irc channel (.trim content)))
      (if (empty? topic)
	(ircb/send-message irc channel (str nick ": I can't help you, I'm afraid. You can only help yourself."))
	(ircb/send-message irc channel (str "Topic: \"" topic "\" doesn't exist!"))))))


(defplugin
 {
  "addtopic" :addtopic
  "rmtopic" :rmtopic
  "help" :help
  "list" :list
 })
