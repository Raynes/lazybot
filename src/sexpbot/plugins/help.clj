;; Written by Erik (boredomist)
(ns sexpbot.plugins.help
  (:use [sexpbot respond info gist utilities]
	stupiddb.core)
  (:require [irclj.irclj :as ircb]))

(let [info (read-config)]
  (def admin-add? (:admin-add? info))
  (def admin-rm? (:admin-rm? info)))

(def help-file (str sexpdir "/help.db"))
(def db (db-init help-file 1800))

;; shamelessly stolen from info.clj's read-config
(defn read-db [& {:keys [string?] :or {string? false}}]
  (let [file (slurp help-file)]
    (if string? file (read-string file))))

(defplugin
  (:addtopic
   "Adds a topic to the help DB. You may have to be an admin to do this."
   ["addtopic"]
   [{:keys [irc nick channel args]}]
   (let [topic (str " " (first args))
	 content (->> args
		      (interpose " ")
		      (rest)
		      (apply str))]
     (cond
      (not-empty (db-get db (.trim topic))) (ircb/send-message irc channel "Topic already exists!")
      (or (empty? (.trim topic))
	  (empty? (.trim content))) (ircb/send-message irc channel "Neither topic nor content can be empty!")
      :else  (if admin-add?
	       (if (= :admin (get-priv nick))
		 (do
		   (db-assoc db (.trim topic) content)
		   (flush-db db)
		   (ircb/send-message irc channel (str "Topic Added: " (.trim topic))))
		 (ircb/send-message irc channel (str nick ": Only admins can add topics!")))
	       (do
		 (db-assoc db (.trim topic) content)
		 (ircb/send-message irc channel (str "Topic Added: " (.trim topic))))))))

  (:rmtopic
   "Removes a topic from the help DB. You may need to be an admin to do this"
   ["rmtopic"]
   [{:keys [irc nick channel args]}]
   (let [topic (first args)]
     (if (not-empty (db-get db topic))
       (if admin-rm?
	 (if (= :admin (get-priv nick))
	   (do
	     (db-dissoc db topic)
	     (flush-db db)
	     (ircb/send-message irc channel (str "Topic Removed: " topic)))
	   (ircb/send-message irc channel (str nick ": Only admins can remove topics!")))
	 (do
	   (db-dissoc db topic)
	   (ircb/send-message irc channel (str "Topic Removed: " topic))))
       (ircb/send-message irc channel (str "Topic: \"" topic  "\" doesn't exist!")))))
  
  (:help-
   "Gives help information on a topic passed to it"
   ["help-"]
   [{:keys [irc nick channel args]}]
   (let [topic (first args)
	 content (db-get db topic)]
     (if (not-empty content)
       (do (ircb/send-message irc channel (str topic ":"))
	   (ircb/send-message irc channel (.trim content)))
       (if (empty? topic)
	 (ircb/send-message irc channel (str nick ": I can't help you, I'm afraid. You can only help yourself."))
	 (ircb/send-message irc channel (str "Topic: \"" topic "\" doesn't exist!"))))))
  
  (:list
   "Lists the available help topics in the DB."
   ["list"]
   [{:keys [irc channel]}]
   (ircb/send-message irc channel (str "I know: " (->> (read-db)
						       keys
						       (interpose " ")
						       (apply str)))))
  (:cleanup (fn [] (db-close db))))
