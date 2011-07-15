; Written by Pepijn de Vos <pepijndevos@gmail.com>
; Licensed under the EPL
(ns lazybot.plugins.ping
  (:use lazybot.registry
        [lazybot.utilities :only [format-time]]))

(def pings (ref {}))

(defn notify-pingers [{:keys [irc bot] :as com-m} nick]
  (doseq [[who when] (dosync
                      (when-let [pingers (@pings nick)]
                        (alter pings dissoc nick)
                        pingers))]
    (let [what (str nick " is available, "
                    (format-time (- (System/currentTimeMillis) when))
                    " after your ping.")]
      (io! (send-message (assoc com-m :channel who) what :notice? true)))))

(defn scan-ping-request [message from]
  (when-let [[_ to] (re-find #"^([^ ]+).{2}ping.?$" message)]
    (dosync
     (alter pings update-in [to]
            assoc
            from (System/currentTimeMillis)))))

(defplugin :irc
  (:hook
   :on-message
   (fn [{:keys [nick message] :as com-m}]
     (notify-pingers com-m nick)
     (scan-ping-request message nick))))