;; Written by Erik (boredomist)
(ns lazybot.plugins.help
  (:require [lazybot.registry :as registry]
            [lazybot.info :as info]
            [lazybot.utilities :refer [prefix]]
            [lazybot.plugins.login :refer [when-privs]]
            [clojure.string :refer [join]]
            [somnium.congomongo :refer [fetch fetch-one insert! destroy!]]))

(registry/defplugin
  (:cmd
   "Adds a topic to the help DB. You may have to be an admin to do this."
   #{"addtopic"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [[topic & content] args
           content-s (join " " content)
           admin-add? (get-in @bot [:config :help :admin-add?])]
       (cond
        (fetch-one :help :where {:topic topic}) (registry/send-message com-m "Topic already exists!")
        (or (empty? topic) (empty? content)) (registry/send-message com-m "Neither topic nor content can be empty!")
        :else (letfn [(insert-and-reply
                       [topic content]
                       (insert! :help {:topic topic :content content-s})
                       (registry/send-message com-m (str "Topic Added: " topic)))]
                (if admin-add?
                  (when-privs com-m :admin (insert-and-reply topic content))
                  (insert-and-reply topic content)))))))

  (:cmd
   "Removes a topic from the help DB. You may need to be an admin to do this"
   #{"rmtopic"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [topic (first args)
           admin-rm? (get-in @bot [:config :help :admin-rm?])]
       (if (fetch-one :help :where {:topic topic})
         (letfn [(destroy-and-reply
                  [topic]
                  (destroy! :help {:topic topic})
                  (registry/send-message com-m (str "Topic Removed: " topic)))]
           (if admin-rm?
             (when-privs com-m :admin (destroy-and-reply topic))
             (destroy-and-reply topic)))
         (registry/send-message com-m (str "Topic: \"" topic  "\" doesn't exist!"))))))

  (:cmd
   "Get help with commands and stuff."
   #{"help"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [help-msg (join
                     " "
                     (filter
                      seq 
                      (.split 
                       (apply str (remove #(= \newline %) (registry/find-docs bot (first args)))) " ")))]
       (if-not (seq help-msg)
         (let [topic (first args)
               content (fetch-one :help :where {:topic topic})]
           (cond
            (not topic) (registry/send-message com-m "You're going to need to tell me what you want help with.")
            content (registry/send-message com-m (prefix nick (:content content)))
            :else (registry/send-message com-m (str "Topic: \"" topic "\" doesn't exist!"))))
         (registry/send-message com-m (prefix nick help-msg))))))

  (:cmd
   "Lists the available help topics in the DB."
   #{"list"}
   (fn [com-m]
     (registry/send-message com-m (str "I know about these topics: "
                                        (->> (fetch :help)
                                             (map :topic)
                                             (join " "))))))
  (:indexes [[:topic]]))
