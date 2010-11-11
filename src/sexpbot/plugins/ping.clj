; Written by Pepijn de Vos <pepijndevos@gmail.com>
; Licensed under the EPL
(ns sexpbot.plugins.ping
  (:use [sexpbot respond info]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin
  (:hook :on-message
     (fn [{:keys [irc bot channel nick message]}]
       (when-let [ping (fetch-one :ping :where {:to nick})]
         (destroy! :ping {:to nick})
         (send-message irc bot (:from ping)
                       (str nick " is available, and took "
                            (- (System/currentTimeMillis)
                               (:time ping))
                            "ms")))
       (when-let [[_ to] (re-find #"^(\w+).{1,2}ping!?$" message)]
         (insert! :ping {:from nick
                         :to to
                         :time (System/currentTimeMillis)})))))
