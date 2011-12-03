(ns lazybot.plugins.knowledge
  (:use lazybot.registry
        socrates.core
        [lazybot.utilities :only [prefix]])
  (:require [socrates.api.direct-answer :as soc]))

(defplugin
  (:cmd
   "Ask me a question."
   #{"know"}
   (fn [{:keys [bot channel nick args] :as com-m}]
     (send-message
      com-m
      (prefix nick
              (let [account (get-in @bot [:config :knowledge :account-id])
                    password (get-in @bot [:config :knowledge :password])
                    question (apply str (interpose " " args))]
                (with-credentials account password
                  (if-let [answer (soc/direct-answer question)]
                    (if (:answered answer)
                      (:result answer)
                      "You've asked the unanswerable.")))))))))

              