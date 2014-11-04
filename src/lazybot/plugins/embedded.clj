(ns lazybot.plugins.embedded
  (:require [lazybot.registry :as registry]))

;; FIXME this is totally broken, expands to a call to second with two args
;;; (second (str (first (:prepends (:config (clojure.core/deref bot))))) x)
(registry/defplugin
  (:hook
   :privmsg
   (fn [{:keys [message bot] :as irc-map}]
     (doseq [x (reverse (re-seq #"\$#(.*?)#\$" message))]
       (->> x
            second
            (-> @bot :config :prepends first str)
            (assoc irc-map :message)
            registry/try-handle)))))
