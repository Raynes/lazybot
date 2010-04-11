(ns sexpbot.plugins.mail
  (:use [sexpbot respond info]
	[clj-time core format]))

(def mailfile (str sexpdir "/mail.clj"))

(def alerted (ref {}))

(defn new-message [from to text]
  (with-info mailfile
    (let [messages (read-config)
	  time (unparse (formatters :date-time-no-ms) (now))] 
      (write-config (assoc messages to (conj (messages to) {:from from 
							    :message text
							    :timestamp time}))))))

(defn compose-message [{:keys [from message timestamp]}]
  (str "From: " from ", Time: " timestamp ", Text: " message))

(defn get-messages [user]
  (with-info mailfile
    (let [messages (read-config)
	  mlist (map compose-message (messages user))]
      (remove-key user)
      mlist)))

(defn count-messages [user]
  (with-info mailfile
    (count ((read-config) user))))

(defn alert-time? [user]
  (if-let [usertime (@alerted (.toLowerCase user))]
    (< 30 (-> usertime (interval (now)) in-secs))
    true))

(defmethod respond :mailalert [{:keys [bot channel sender]}]
  (let [lower-sender (.toLowerCase sender)
	nmess (count-messages lower-sender)]
    (when (and (> nmess 0) (alert-time? lower-sender))
      (.sendMessage bot channel (str sender ": You have " nmess 
				     " new messages. Type $getmessages to see them."))
      (dosync (alter alerted assoc lower-sender (now))))))

(defmethod respond :getmessages [{:keys [bot sender]}]
  (let [lower-sender (.toLowerCase sender)]
    (if-let [messages (seq (get-messages lower-sender))]
      (doseq [message messages] (.sendMessage bot lower-sender message))
      (.sendMessage bot sender "You have no messages."))))

(defmethod respond :mail [{:keys [bot channel sender args server]}]
  (if (seq args)
    (let [lower-user (.toLowerCase (first args))]
      (if (and (not (.contains lower-user "serv"))
	       (not= lower-user (.toLowerCase (((read-config) :bot-name) server))))
	(do
	  (new-message sender lower-user 
		       (->> args rest (interpose " ") (apply str)))
	  (.sendMessage bot channel "Message saved."))
	(.sendMessage bot channel "You can't message the unmessageable.")))))

(defplugin
  {"mailalert"   :mailalert
   "getmessages" :getmessages
   "mail"        :mail})