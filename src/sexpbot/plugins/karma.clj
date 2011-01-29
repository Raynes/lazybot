; Written by Michael D. Ivey <ivey@gweezlebur.com>
; Licensed under the EPL

(ns sexpbot.plugins.karma
  (:use [sexpbot registry info]
        [sexpbot.utilities :only [keywordize]]
	[somnium.congomongo :only [fetch-one insert! destroy!]]))

(defn- set-karma
  [nick server channel karma]
  (let [nick (.toLowerCase nick)
        attrs (keywordize [nick server channel])]
    (destroy! :karma attrs)
    (insert! :karma (assoc attrs :karma karma))))

(defn- get-karma
  [nick server channel]
  (let [nick (.toLowerCase nick)
        user-map (fetch-one :karma
                            :where (keywordize [nick server channel]))]
    (get user-map :karma 0)))

(defn- put-karma [{:keys [channel com]} nick karma]
  (set-karma nick (:server @com) channel karma))

(def limit (ref {}))

;; TODO: mongo has atomic inc/dec commands - we should use those
(defn- change-karma
  [snick new-karma {:keys [nick com bot channel] :as com-m}]
  (let [[msg apply] 
        (dosync
         (let [current (get-in @limit [nick snick])]
           (cond
            (= nick snick) ["You can't adjust your own karma."]
            (= current 3) ["Do I smell abuse? Wait a while before modifying that person's karma again."]
            (= current new-karma) ["You want me to leave karma the same? Fine, I will."]
            :else [(str "\u27F9 " new-karma)
                   (alter limit update-in [nick snick] (fnil inc 0))])))]
    (when apply
      (put-karma com-m snick new-karma)
      (future (Thread/sleep 300000)
              (alter limit update-in [nick snick] dec)))
    (send-message com-m msg)))

(defn karma-fn
  "Create a plugin command function that applies f to the karma of the user specified in args."
  [f]
  (fn [{:keys [com channel args] :as com-m}]
    (let [snick (first args)
          karma (get-karma snick (:server @com) channel)
          new-karma (f karma)]
      (change-karma snick new-karma com-m))))

(defplugin :irc
  (:hook :on-message
         (fn [{:keys [message] :as com-m}]
           (let [[_ direction snick] (re-find #"^\((inc|dec) (.+)\)$" message)]
             (when snick
               ((karma-fn (case direction
                                "inc" inc
                                "dec" dec))
                (merge com-m {:args [snick]}))))))
  (:cmd
   "Checks the karma of the person you specify."
   #{"karma"} 
   (fn [{:keys [com bot channel args] :as com-m}]
     (let [nick (first args)]
       (send-message com-m
                     (if-let [karma (get-karma nick (:server @com-m) channel)]
                       (str nick " has karma " karma ".")
                       (str "I have no record for " nick "."))))))
  (:cmd
   "Increases the karma of the person you specify."
   #{"inc"} (karma-fn inc))
  (:cmd
   "Decreases the karma of the person you specify."
   #{"dec"} (karma-fn dec))
  (:indexes [[:server :channel :nick]]))
