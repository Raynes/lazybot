; Written by Your Mom
; Licensed under Your Face

(ns lazybot.plugins.morning
  (:use [lazybot registry info]
        [lazybot.utilities :only [prefix]]))

(def responses ["Let's not talk about what time it is for who, kthx."
                "Yes, everyone lives in a different time zone. Can we drop it now?"
                "Okay, we get it, timezones are funny. Let's move on."
                "Quick, nobody mention UGT!"])

(defplugin
  (:hook
   :on-message
   (fn [{:keys [com bot nick message channel] :as com-m}]
     (if (re-find #"(?i)\b(morning|afternoon|evening|night|day)\b" message)
       (send-message com-m (prefix nick (rand-nth responses)))))))
