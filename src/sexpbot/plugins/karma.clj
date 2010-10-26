; Written by Michael D. Ivey <ivey@gweezlebur.com>
; Licensed under the EPL

(ns sexpbot.plugins.karma
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[somnium.congomongo :only [fetch-one insert! destroy!]]))

(defn set-karma
  [nick server channel karma]
  (let [lower-nick (.toLowerCase nick)]
    (destroy! :karma {:nick lower-nick :server server :channel channel})
    (insert! :karma
             {:server server
              :channel channel 
              :karma karma
              :nick lower-nick})))

(defn get-karma
  "Gets the karma for a nick."
  [nick server channel]
  (let [lower-nick (.toLowerCase nick)
        user-map (fetch-one :karma :where {:nick lower-nick :server server :channel channel})]
    (if (not-empty user-map)
      (:karma user-map)
      1)))

(defn put-karma [{:keys [channel irc]} nick karma] (set-karma nick (:server @irc) channel karma))

(defplugin
  (:hook :on-message
         (fn [{:keys [irc bot message nick channel] :as irc-map}]
           (let [server (:server @irc)
                 karma-command (re-find #"^\((inc|dec) ([^/]*)\)" message)]
             (when (not-empty karma-command)
               (let [[_ direction snick] karma-command
                     karma (get-karma snick server channel)
                     new-karma (case
                                direction
                                "inc" (+ karma 1)
                                "dec" (- karma 1)
                                karma)]
                 (if (= nick snick)
                   (send-message irc bot channel "You can't adjust your own karma.")
                   (do
                     (put-karma irc-map snick new-karma)
                     (send-message irc bot channel (str "=> " new-karma)))))))))
  (:cmd
   "Checks the karma of the person you specify."
   #{"karma"} 
   (fn [{:keys [irc bot nick channel args]}]
     (if-let [karma (get-karma (first args) (:server @irc) channel)]
       (send-message irc bot channel (str (first args) " has karma " karma "."))
       (send-message irc bot channel (str "I have no record for " (first args) "."))))))