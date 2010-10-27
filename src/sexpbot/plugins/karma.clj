; Written by Michael D. Ivey <ivey@gweezlebur.com>
; Licensed under the EPL

(ns sexpbot.plugins.karma
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[somnium.congomongo :only [fetch-one insert! destroy!]]))

(defn- set-karma
  [nick server channel karma]
  (let [lower-nick (.toLowerCase nick)]
    (destroy! :karma {:nick lower-nick :server server :channel channel})
    (insert! :karma
             {:server server
              :channel channel 
              :karma karma
              :nick lower-nick})))

(defn- get-karma
  [nick server channel]
  (let [lower-nick (.toLowerCase nick)
        user-map (fetch-one :karma :where {:nick lower-nick :server server :channel channel})]
    (if (not-empty user-map)
      (:karma user-map)
      0)))

(defn- put-karma [{:keys [channel irc]} nick karma] (set-karma nick (:server @irc) channel karma))

(def limit (atom {}))

(defn- change-karma
  [snick new-karma {:keys [nick irc bot channel] :as irc-map}]
  (let [current (get-in @limit [nick snick])]
    (cond
     (= nick snick) (send-message irc bot channel "You can't adjust your own karma.")
     (= 3 current)
     (send-message irc bot channel
                   "Do I smell abuse? Wait a while before modifying that persons karma again.")
     :else (do
             (put-karma irc-map snick new-karma)
             (swap! limit update-in [nick snick] #(if (nil? %) 1 (inc %)))
             (future (Thread/sleep 300000) (swap! limit update-in [nick snick] dec))
             (send-message irc bot channel (str "=> " new-karma))))))

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
                 (change-karma snick new-karma irc-map))))))
  (:cmd
   "Checks the karma of the person you specify."
   #{"karma"} 
   (fn [{:keys [irc bot nick channel args]}]
     (if-let [karma (get-karma (first args) (:server @irc) channel)]
       (send-message irc bot channel (str (first args) " has karma " karma "."))
       (send-message irc bot channel (str "I have no record for " (first args) ".")))))
  (:cmd
   "Increases the karma of the person you specify."
   #{"inc"} 
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (let [snick (first args)
           karma (get-karma snick (:server @irc) channel)
           new-karma (+ karma 1)]
       (change-karma snick new-karma irc-map))))
  (:cmd
   "Decreases the karma of the person you specify."
   #{"dec"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (let [snick (first args)
           karma (get-karma snick (:server @irc) channel)
           new-karma (- karma 1)]
       (change-karma snick new-karma irc-map)))))