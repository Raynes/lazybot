(ns lazybot.plugins.autoreply
  (:require [lazybot.registry :as registry]
            [clojure.string :as s]))

(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [bot com message channel] :as com-m}]
     (when-let [reply (first
                       (for [[find replace]
                             (get-in @bot [:config (:server @com) :autoreply :autoreplies channel])
                             :when (re-find find message)]
                         (s/replace message find replace)))]
       (registry/send-message com-m reply)))))
