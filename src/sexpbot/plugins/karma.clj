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

(defn- put-karma [{:keys [channel irc]} nick karma]
  (set-karma nick (:server @irc) channel karma))

(def limit (ref {}))

;; TODO: mongo has atomic inc/dec commands - we should use those
(defn- change-karma
  [snick new-karma {:keys [nick irc bot channel] :as irc-map}]
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
      (put-karma irc-map snick new-karma)
      (future (Thread/sleep 300000)
              (alter limit update-in [nick snick] dec)))
    (send-message irc bot channel msg)))

(defn karma-fn
  "Create a plugin command function that applies f to the karma of the user specified in args."
  [f]
  (fn [{:keys [irc channel args] :as irc-map}]
    (let [snick (first args)
          karma (get-karma snick (:server @irc) channel)
          new-karma (f karma)]
      (change-karma snick new-karma irc-map))))

(defplugin
  (:hook :on-message
         (fn [{:keys [message irc channel] :as irc-map}]
           (let [[_ direction snick] (re-find #"^\((inc|dec) (.+)\)$" message)]
             (when snick
               ((karma-fn (case direction
                                "inc" inc
                                "dec" dec))
                (merge irc-map {:args [snick]}))))))
  (:cmd
   "Checks the karma of the person you specify."
   #{"karma"} 
   (fn [{:keys [irc bot channel args]}]
     (let [nick (first args)]
       (send-message irc bot channel
                     (if-let [karma (get-karma nick (:server @irc) channel)]
                       (str nick " has karma " karma ".")
                       (str "I have no record for " nick "."))))))
  (:cmd
   "Increases the karma of the person you specify."
   #{"inc"} (karma-fn inc))
  (:cmd
   "Decreases the karma of the person you specify."
   #{"dec"} (karma-fn dec)))
