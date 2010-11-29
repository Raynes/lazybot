; Written by Pepijn de Vos <pepijndevos@gmail.com>
; Licensed under the EPL
(ns sexpbot.plugins.ping
  (:use sexpbot.registry
        [sexpbot.utilities :only [format-time]]))

(def pings (ref {}))

(defn notify-pingers [{:keys [irc bot]} nick]
  (doseq [[who when] (dosync
                      (when-let [pingers (@pings nick)]
                        (alter pings dissoc nick)
                        pingers))]
    (let [what (str nick " is available, "
                    (format-time (- (System/currentTimeMillis) when))
                    " after your ping.")]
      (io! (send-message irc bot who what :notice? true)))))

(defn scan-ping-request [message from]
  (when-let [[_ to] (re-find #"^([^ ]+).{2}ping.?$" message)]
    (dosync
     (alter pings update-in [to]
            assoc
            from (System/currentTimeMillis)))))

(defplugin :irc
  (:hook
   :on-message
   (fn [{:keys [nick message] :as args}]
     (notify-pingers args nick)
     (scan-ping-request message nick))))