(ns lazybot.plugins.autoreply
  (:use (lazybot registry info))
  (:require [clojure.string :as s]))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [bot com message channel] :as com-m}]

     (when-let [reply (first (for [[find replace] (get-in @bot
                                                          [:config (:server @com) :autoreplies channel])
                                   :when (re-find find message)]
                               (s/replace message find replace)))]
       (send-message com-m reply)))))