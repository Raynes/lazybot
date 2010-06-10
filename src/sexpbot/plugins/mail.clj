(ns sexpbot.plugins.mail
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[clj-time core format]
	clj-config.core)
  (:require [irclj.irclj :as ircb]))

(def mailfile (str sexpdir "/mail.clj"))

(def alerted (ref {}))

(defn new-message [from to text]
  (let [messages (read-config mailfile)
	time (unparse (formatters :date-time-no-ms) (now))] 
    (write-config (assoc messages to (conj (messages to) {:from from 
							  :message text
							  :timestamp time}))
		  mailfile)))

(defn compose-message [{:keys [from message timestamp]}]
  (str "From: " from ", Time: " timestamp ", Text: " message))

(defn get-messages* [user]
  (let [messages (read-config mailfile)
	mlist (map compose-message (messages user))]
    (remove-key user mailfile)
    mlist))

(defn count-messages [user]
  (count ((read-config mailfile) user)))

(defn alert-time? [user]
  (if-let [usertime (@alerted (.toLowerCase user))]
    (< 30 (-> usertime (interval (now)) in-secs))
    true))

(defn mail-alert
  [{:keys [irc channel nick]}]
  (let [lower-nick (.toLowerCase nick)
	nmess (count-messages lower-nick)]
    (when (and (> nmess 0) (alert-time? lower-nick))
      (ircb/send-notice irc nick (str nick ": You have " nmess 
				      " new message(s). Type $mymail or $mail without any arguments to see them. This also works via PM."))
      (dosync (alter alerted assoc lower-nick (now))))))

(defn get-messages [irc nick]
  (let [lower-nick (.toLowerCase nick)]
    (if-let [messages (seq (get-messages* lower-nick))]
      (doseq [message messages] (ircb/send-message irc lower-nick message))
      (ircb/send-message irc nick "You have no messages."))))

(defplugin
  (:add-hook :on-message (fn [irc-map] (mail-alert irc-map)))
  
  (:getmessages 
   "Request that your messages be sent you via PM. Executing this command will delete all your messages."
   ["getmessages" "getmail" "mymail"] 
   [{:keys [irc nick]}]
    (get-messages irc nick))

  (:mail 
   "Send somebody a message. Takes a nickname and a message to send. Will alert the person with a notice."
   ["mail"]
   [{:keys [irc channel nick args irc]}]
   (if (seq args)
     (let [lower-user (.toLowerCase (first args))]
       (if (and (not (.contains lower-user "serv"))
		(not= lower-user (.toLowerCase (:bot-name ((read-config info-file) (:server @irc))))))
	  (do
	    (new-message nick lower-user 
			 (->> args rest (interpose " ") (apply str)))
	    (ircb/send-message irc channel "Message saved."))
	  (ircb/send-message irc channel "You can't message the unmessageable.")))
     (get-messages irc nick))))