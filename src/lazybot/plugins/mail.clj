(ns lazybot.plugins.mail
  (:refer-clojure :exclude [extend])
  (:use [lazybot registry info]
        [lazybot.plugins.login :only [when-privs]]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]])
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(def alerted (atom {}))

(defn new-message [from to text]
  (let [time (f/unparse (f/formatters :date-time-no-ms) (t/now))]
    (insert! :mail {:to to
                    :from from
                    :message text
                    :timestamp time})))

(defn compose-message [{:keys [from message timestamp]}]
  (str "From: " from ", Time: " timestamp ", Text: " message))

(defn destroy-messages! [com-m from to]
  (send-message com-m (str "Deleted unread messages from " from " to " to))
  (destroy! :mail {:to to :from from}))

(defn fetch-messages [user]
  (let [mlist (doall (map compose-message (fetch :mail :where {:to user})))]
    (destroy! :mail {:to user})
    (swap! alerted dissoc user)
    mlist))

(defn count-messages [user]
  (count (fetch :mail :where {:to user})))

(defn alert-time? [user]
  (if-let [usertime (@alerted (.toLowerCase user))]
    (< 300 (-> usertime (t/interval (t/now)) t/in-secs))
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
      (swap! alerted assoc lower-nick (t/now)))))

(defn get-messages [{:keys [nick] :as com-m}]
  (let [lower-nick (.toLowerCase nick)]
    (if-let [messages (seq (fetch-messages lower-nick))]
      (doseq [message messages] (send-message (assoc com-m :channel lower-nick) message))
      (send-message com-m "You have no messages."))))

(defplugin
  (:hook :on-message #'mail-alert)
  (:hook :on-join #'mail-alert)

  (:cmd
   "Request that your messages be sent you via PM. Executing this command will delete all your messages."
   #{"getmessages" "getmail" "mymail"}
   #'get-messages)

  (:cmd
   "Send somebody a message. Takes a nickname and a message to send. Will alert the person with a notice."
   #{"mail"}
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
  (:cmd
   "Cancel your pending messages to another user. Specify the user's nick. Admin users can use $unmail <from> <to> to delete anyone's mails."
   #{"unmail"}
   (fn [{:keys [com nick args irc] :as com-m}]
     (if (> (count args) 1)
       (when-privs com-m :admin
         (let [[from to] args]
           (destroy-messages! com-m from to)))
       (destroy-messages! com-m nick (first args)))))
  (:indexes [[:to :from]]))
