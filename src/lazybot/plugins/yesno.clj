(ns lazybot.plugins.yesno
  (:use [lazybot registry info]
        [lazybot.utilities :only [prefix]]))

(def answers {:yes ["Oh, absolutely."
                    "How could that be wrong?"
                    "Yes, 100% for sure."]
              :no  ["What are you, crazy? Of course not!"
                    "Definitely not."
                    "Uh, no. Why would you even ask?"]})

(defn choose-answer [num-questions]
  (case num-questions
        3 :yes
        2 :no
        nil))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [com bot nick message channel] :as com-m}]
     (when-let [[match questions] (re-find #"(\?+)\s*$" message)]
       (when-let [answer-type (choose-answer (count questions))]
         (send-message com-m (prefix nick (rand-nth (get answers answer-type)))))))))
