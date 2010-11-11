; Written by Pepijn de Vos <pepijndevos@gmail.com>
; Licensed under the EPL
(ns sexpbot.plugins.ping
  (:refer-clojure :exclude [extend])
  (:use sexpbot.respond [clj-time core]))

(def pings (atom {}))

(defplugin
  (:hook :on-message
     (fn [{:keys [irc bot channel nick message]}]
       (when-let [ping (@pings nick)]
         (swap! pings dissoc nick)
         (send-message irc bot (:from ping)
                       (str nick " is available, "
                            (in-secs (interval (:time ping) (now)))
                            " seconds since your ping.")
                       :notice? true))
       (when-let [[_ to] (re-find #"^([^ ]+).{2}ping!?$" message)]
         (swap! pings assoc to {:from nick :time (now)})))))
