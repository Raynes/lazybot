(ns lazybot.plugins.embedded
  (:require [lazybot.registry :as registry]))

(registry/defplugin
  (:hook
   :privmsg
   (fn [{:keys [message bot] :as irc-map}]
     (doseq [command (reverse (re-seq #"\$#(.*?)#\$" message))]
       (as-> command x
             (second x)
             (str (->  @bot :config :prepends first) x)
             (assoc irc-map :message x)
             (registry/try-handle x))))))
