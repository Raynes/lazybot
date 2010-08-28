;; Written by Erik (boredomist)
(ns sexpbot.plugins.help
  (:use [sexpbot respond info]
	[clj-config.core :only [read-config]]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(let [info (read-config info-file)]
  (def admin-add? (:admin-add? info))
  (def admin-rm? (:admin-rm? info)))

(defplugin
  (:addtopic
   "Adds a topic to the help DB. You may have to be an admin to do this."
   ["addtopic"]
   [{:keys [irc bot nick channel args] :as ircm}]
   (let [[topic & content] args
	 content-s (->> content
                        (interpose " ")
                        (apply str))]
     (cond
      (fetch-one :help :where {:topic topic}) (send-message irc bot channel "Topic already exists!")
      (or (empty? topic) (empty? content)) (send-message irc bot channel "Neither topic nor content can be empty!")
      :else (letfn [(insert-and-reply
                     [topic content]
                     (insert! :help {:topic topic :content content-s})
                     (send-message irc bot channel (str "Topic Added: " topic)))]
              (if admin-add?
                (if-admin nick ircm bot
                          (insert-and-reply topic content))
                (insert-and-reply topic content))))))

  (:rmtopic
   "Removes a topic from the help DB. You may need to be an admin to do this"
   ["rmtopic"]
   [{:keys [irc bot nick channel args] :as ircm}]
   (let [topic (first args)]
     (if (fetch-one :help :where {:topic topic})
       (letfn [(destroy-and-reply
                [topic]
                (destroy! :help {:topic topic})
                (send-message irc bot channel (str "Topic Removed: " topic)))]
         (if admin-rm?
           (if-admin nick ircm bot
                     (destroy-and-reply topic))
           (destroy-and-reply topic)))
       (send-message irc bot channel (str "Topic: \"" topic  "\" doesn't exist!")))))

  (:help
   "Get help with commands and stuff."
   ["help"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (let [help-msg (apply
                   str 
                   (interpose
                    " " 
                    (filter
                     seq 
                     (.split 
                      (apply str (remove #(= \newline %) (find-docs bot (first args)))) " "))))]
     (if-not (seq help-msg)
       (let [topic (first args)
	     content (fetch-one :help :where {:topic topic})]
         (cond
          (not topic) (send-message irc bot channel "You're going to need to tell me what you want help with.")
          content (send-message irc bot channel (str nick ": " (:content content)))
          :else (send-message irc bot channel (str "Topic: \"" topic "\" doesn't exist!"))))
       (send-message irc bot channel (str nick ": " help-msg)))))
  
  (:list
   "Lists the available help topics in the DB."
   ["list"]
   [{:keys [irc bot channel]}]
   (send-message irc bot channel (str "I know about these topics: "
                                      (->> (fetch :help)
                                           (map :topic)
                                           (interpose " ")
                                           (apply str))))))
