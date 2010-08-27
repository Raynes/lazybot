;; Written by Erik (boredomist)
(ns sexpbot.plugins.help
  (:use [sexpbot respond info]
	[clj-config.core :only [read-config]]
	stupiddb.core))

(let [info (read-config info-file)]
  (def admin-add? (:admin-add? info))
  (def admin-rm? (:admin-rm? info)))

(def help-file (str sexpdir "/help.db"))
(def db (db-init help-file 1800))

(defplugin
  (:addtopic
   "Adds a topic to the help DB. You may have to be an admin to do this."
   ["addtopic"]
   [{:keys [irc bot nick channel args] :as ircm}]
   (let [topic (str " " (first args))
	 content (->> args
		      (interpose " ")
		      (rest)
		      (apply str))]
     (cond
      (not-empty (db-get db (.trim topic))) (send-message irc bot channel "Topic already exists!")
      (or (empty? (.trim topic))
	  (empty? (.trim content))) (send-message irc bot channel "Neither topic nor content can be empty!")
      :else  (if admin-add?
	       (if-admin nick ircm bot
                         (do
                           (db-assoc db (.trim topic) content)
                           (send-message irc bot channel (str "Topic Added: " (.trim topic)))))
	       (do
		 (db-assoc db (.trim topic) content)
		 (send-message irc bot channel (str "Topic Added: " (.trim topic))))))))

  (:rmtopic
   "Removes a topic from the help DB. You may need to be an admin to do this"
   ["rmtopic"]
   [{:keys [irc bot nick channel args] :as ircm}]
   (let [topic (first args)]
     (if (not-empty (db-get db topic))
       (if admin-rm?
	 (if-admin nick ircm bot
	   (do
	     (db-dissoc db topic)
	     (send-message irc bot channel (str "Topic Removed: " topic))))
	 (do
	   (db-dissoc db topic)
	   (send-message irc bot channel (str "Topic Removed: " topic))))
       (send-message irc bot channel (str "Topic: \"" topic  "\" doesn't exist!")))))

  (:help
   "Get help with commands and stuff."
   ["help"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (let [help-msg (.trim 
		   (apply
		    str 
		    (interpose
		     " " 
		     (filter
		      seq 
		      (.split 
		       (apply str (remove #(= \newline %) (find-docs bot (first args)))) " ")))))]
     (if-not (seq help-msg)
       (let [topic (first args)
	     content (db-get db topic)]
	 (if (not-empty content)
	   (send-message irc bot channel (str nick ":" (.trim content)))
	   (if (empty? topic)
	     (send-message irc bot channel (str nick ": I can't help you, I'm afraid. You can only help yourself."))
	     (send-message irc bot channel (str "Topic: \"" topic "\" doesn't exist!")))))
       (send-message irc bot channel (str nick ": " help-msg)))))
  
  (:list
   "Lists the available help topics in the DB."
   ["list"]
   [{:keys [irc bot channel]}]
   (send-message irc bot channel (str "I know: " (->> (read-config help-file)
                                                     keys
                                                     (interpose " ")
                                                     (apply str)))))
  (:cleanup (fn [] (db-close db))))
