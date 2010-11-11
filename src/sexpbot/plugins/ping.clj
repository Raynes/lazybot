; Written by Pepijn de Vos <pepijndevos@gmail.com>
; Licensed under the EPL
(ns sexpbot.plugins.ping
  (:refer-clojure :exclude [extend])
  (:use sexpbot.respond
        [clj-time core format]
        [somnium.congomongo :only [fetch-one insert! destroy!]]))

(def basic (formatters :basic-date-time))

(defplugin
  (:hook :on-message
     (fn [{:keys [irc bot channel nick message]}]
       (when-let [ping (fetch-one :ping :where {:to nick})]
         (destroy! :ping {:to nick})
         (send-message irc bot (:from ping)
                       (str nick " is available, "
                            (in-secs (interval (parse basic (:time ping)) (now)))
                            " seconds since your ping.")
                       :notice? true))
       (when-let [[_ to] (re-find #"^(\w+).{1,2}ping!?$" message)]
         (insert! :ping {:from nick
                         :to to
                         :time (unparse basic (now))})))))
