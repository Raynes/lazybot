(ns sexpbot.plugins.mail
  (:use [sexpbot respond info]))

(def mailfile (str sexpdir "/mail.clj"))

(defn new-message [from to text]
  (with-info mailfile
    (let [messages (read-config)] 
      (write-config (assoc messages to (conj (messages to) {:from from :message text}))))))

(defn compose-message [{:keys [from message]}]
  (str "From: " from " Text: " message))

(defn get-messages [user]
  (with-info mailfile
    (let [messages (read-config)
	  mlist (map compose-message (messages user))]
      (remove-key user)
      mlist)))

(defn count-messages [user]
  (with-info mailfile
    (count ((read-config) user))))

(defmethod respond :mailalert [{:keys [bot channel sender]}]
  (let [nmess (count-messages sender)]
    (when (> nmess 0)
      (.sendMessage bot channel (str sender ": You have " nmess 
				     " new messages. Type $getmessages to see them.")))))

(defmethod respond :getmessages [{:keys [bot sender]}]
  (if-let [messages (seq (get-messages sender))]
    (doseq [message messages] (.sendMessage bot sender message))
    (.sendMessage bot sender "You have no messages.")))

(defmethod respond :mail [{:keys [bot channel sender args]}]
  (new-message sender (first args) (->> args rest (interpose " ") (apply str)))
  (.sendMessage bot channel "Message saved."))

(defplugin
  {"mailalert"   :mailalert
   "getmessages" :getmessages
   "mail"        :mail})