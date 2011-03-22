(ns sexpbot.plugins.mail
  (:refer-clojure :exclude [extend])
  (:use [sexpbot registry info]
	[clj-time core format]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(def alerted (atom {}))

(defn new-message [from to text]
  (let [time (unparse (formatters :date-time-no-ms) (now))] 
    (insert! :mail {:to to
                    :from from 
                    :message text
                    :timestamp time})))

(defn compose-message [{:keys [from message timestamp]}]
  (str "From: " from ", Time: " timestamp ", Text: " message))

(defn fetch-messages [user]
  (let [mlist (doall (map compose-message (fetch :mail :where {:to user})))]
    (destroy! :mail {:to user})
    mlist))

(defn count-messages [user]
  (count (fetch :mail :where {:to user})))

(defn alert-time? [user]
  (if-let [usertime (@alerted (.toLowerCase user))]
    (< 300 (-> usertime (interval (now)) in-secs))
    true))

(defn mail-alert
  [{:keys [nick bot] :as com-m}]
  (let [lower-nick (.toLowerCase nick)
	nmess (count-messages lower-nick)]
    (when (and (> nmess 0) (alert-time? lower-nick))
      (send-message
       (assoc com-m :channel nick)
       (str "You have " nmess 
            " new message(s). To retrieve your mail, send me a private message with the contents 'mail'.")
       :notice? true)
      (swap! alerted assoc lower-nick (now)))))

(defn get-messages [{:keys [nick] :as com-m}]
  (let [lower-nick (.toLowerCase nick)]
    (if-let [messages (seq (fetch-messages lower-nick))]
      (doseq [message messages] (send-message (assoc com-m :channel lower-nick) message))
      (send-message com-m "You have no messages."))))

(defplugin
  (:hook :on-message :irc #'mail-alert)
  (:hook :on-join #'mail-alert)
  
  (:cmd
   "Request that your messages be sent you via PM. Executing this command will delete all your messages."
   #{"getmessages" "getmail" "mymail"}
   :irc
   #'get-messages)

  (:cmd 
   "Send somebody a message. Takes a nickname and a message to send. Will alert the person with a notice."
   #{"mail"}
   :irc
   (fn [{:keys [com nick args irc] :as com-m}]
     (if (seq args)
       (let [lower-user (.toLowerCase (first args))]
         (if (and (not (.contains lower-user "serv"))
                  (not= lower-user (.toLowerCase (:name @com))))
           (do
             (new-message nick lower-user (->> args rest (interpose " ") (apply str)))
             (send-message com-m "Message saved."))
           (send-message com-m "You can't message the unmessageable.")))
       (get-messages com-m))))
  (:indexes [[:to]]))
