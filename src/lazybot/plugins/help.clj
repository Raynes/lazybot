;; Written by Erik (boredomist)
(ns lazybot.plugins.help
  (:use [lazybot registry info]
        [lazybot.plugins.login :only [when-privs]]
        [clojure.string :only [join]]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin
  (:cmd
   "Adds a topic to the help DB. You may have to be an admin to do this."
   #{"addtopic"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [[topic & content] args
           content-s (join " " content)
           admin-add? (:admin-add? (:config @bot))]
       (cond
        (fetch-one :help :where {:topic topic}) (send-message com-m "Topic already exists!")
        (or (empty? topic) (empty? content)) (send-message com-m "Neither topic nor content can be empty!")
        :else (letfn [(insert-and-reply
                       [topic content]
                       (insert! :help {:topic topic :content content-s})
                       (send-message com-m (str "Topic Added: " topic)))]
                (if admin-add?
                  (when-privs com-m :admin (insert-and-reply topic content))
                  (insert-and-reply topic content)))))))

  (:cmd
   "Removes a topic from the help DB. You may need to be an admin to do this"
   #{"rmtopic"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [topic (first args)
           admin-rm? (:admin-rm? (:config @bot))]
       (if (fetch-one :help :where {:topic topic})
         (letfn [(destroy-and-reply
                  [topic]
                  (destroy! :help {:topic topic})
                  (send-message com-m (str "Topic Removed: " topic)))]
           (if admin-rm?
             (when-privs com-m :admin (destroy-and-reply topic))
             (destroy-and-reply topic)))
         (send-message com-m (str "Topic: \"" topic  "\" doesn't exist!"))))))

  (:cmd
   "Get help with commands and stuff."
   #{"help"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [help-msg (join
                     " "
                     (filter
                      seq 
                      (.split 
                       (apply str (remove #(= \newline %) (find-docs bot (first args)))) " ")))]
       (if-not (seq help-msg)
         (let [topic (first args)
               content (fetch-one :help :where {:topic topic})]
           (cond
            (not topic) (send-message com-m "You're going to need to tell me what you want help with.")
            content (send-message com-m (prefix bot nick (:content content)))
            :else (send-message com-m (str "Topic: \"" topic "\" doesn't exist!"))))
         (send-message com-m (prefix bot nick help-msg))))))
  
  (:cmd
   "Lists the available help topics in the DB."
   #{"list"}
   (fn [com-m]
     (send-message com-m (str "I know about these topics: "
                                        (->> (fetch :help)
                                             (map :topic)
                                             (join " "))))))
  (:indexes [[:topic]]))
