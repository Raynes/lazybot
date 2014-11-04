(ns lazybot.plugins.knowledge
  (:require [lazybot.registry :as registry]
            [socrates.core :as socrates]
            [lazybot.utilities :refer [prefix]]
            [lazybot.paste :refer [trim-with-paste]]
            [socrates.api.direct-answer :as soc]))

(registry/defplugin
  (:cmd
   "Ask me a question."
   #{"know"}
   (fn [{:keys [bot channel user-nick args] :as com-m}]
     (registry/send-message
      com-m
      (prefix user-nick
              (let [account (get-in @bot [:config :knowledge :account-id])
                    password (get-in @bot [:config :knowledge :password])
                    question (apply str (interpose " " args))]
                (socrates/with-credentials account password
                  (if-let [answer (soc/direct-answer question)]
                    (if (:answered answer)
                      (trim-with-paste (:result answer))
                      "You've asked the unanswerable.")))))))))


