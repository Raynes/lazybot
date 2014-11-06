(ns lazybot.plugins.autoreply
  (:require [lazybot.registry :as registry]
            [clojure.string :as s]))

(registry/defplugin
  (:hook
   :privmsg
   (fn [{:keys [bot com message channel network] :as com-m}]
     (let [autos (get-in @bot [:config network :autoreply :autoreplies channel])]
       (when-let [reply (first
                         (for [[find replace] autos
                               :when (re-find find message)]
                           (s/replace message find replace)))]
         (registry/send-message com-m reply))))))
