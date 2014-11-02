(ns lazybot.plugins.embedded
  (:require [lazybot.registry :as registry]))

(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [message bot] :as irc-map}]
     (doseq [x (reverse (re-seq #"\$#(.*?)#\$" message))]
       (->> x
            second
            (-> @bot :config :prepends first str)
            (assoc irc-map :message)
            registry/try-handle)))))
