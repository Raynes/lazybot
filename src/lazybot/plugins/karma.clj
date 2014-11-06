; Written by Michael D. Ivey <ivey@gweezlebur.com>
; Licensed under the EPL

(ns lazybot.plugins.karma
  (:require [lazybot.registry :as registry]
            [lazybot.info :as info]
            [useful.map :refer [keyed]]
            [somnium.congomongo :refer [fetch-one insert! update!]])
  (:import (java.util.concurrent Executors ScheduledExecutorService TimeUnit)))

(defn- key-attrs [nick server channel]
  (let [nick (.toLowerCase nick)]
    (keyed [nick server channel])))

(defn- set-karma
  [nick server channel karma]
  (let [attrs (key-attrs nick server channel)]
    (update! :karma attrs (assoc attrs :karma karma))))

(defn- get-karma
  [nick server channel]
  (let [user-map (fetch-one :karma
                            :where (key-attrs nick server channel))]
    (get user-map :karma 0)))

(def limit (ref {}))

(let [scheduler (Executors/newScheduledThreadPool 1)]
  (defn schedule [^Runnable task]
    (.schedule scheduler task
               5 TimeUnit/MINUTES)))

;; TODO: mongo has atomic inc/dec commands - we should use those
(defn- change-karma
  [snick new-karma {:keys [^String nick com bot channel] :as com-m}]
  (let [[msg apply]
        (dosync
         (let [current (get-in @limit [nick snick])]
           (cond
            (.equalsIgnoreCase nick snick) ["You can't adjust your own karma."]
            (= current 3) ["Do I smell abuse? Wait a while before modifying that person's karma again."]
            (= current new-karma) ["You want me to leave karma the same? Fine, I will."]
            :else [(str (get-in @bot [:config :prefix-arrow]) new-karma)
                   (alter limit update-in [nick snick] (fnil inc 0))])))]
    (when apply
      (set-karma snick (:network @com) channel new-karma)
      (schedule #(dosync (alter limit update-in [nick snick] dec))))
    (registry/send-message com-m msg)))

(defn karma-fn
  "Create a plugin command function that applies f to the karma of the user specified in args."
  [f]
  (fn [{:keys [network channel args] :as com-m}]
    (let [snick (first args)
          karma (get-karma snick network channel)
          new-karma (f karma)]
      (change-karma snick new-karma com-m))))

(def print-karma
  (fn [{:keys [network bot channel args] :as com-m}]
    (let [nick (first args)]
      (registry/send-message
       com-m
       (if-let [karma (get-karma nick network  channel)]
         (str nick " has karma " karma ".")
         (str "I have no record for " nick "."))))))

(registry/defplugin
  (:hook
   :privmsg
   (fn [{:keys [message] :as com-m}]
     (let [[_ direction snick] (re-find #"^\((inc|dec|identity)\s*([^)]+)\s*\)(\s*;.*)?$" message)]
       (when snick
         ((case direction
            "inc" (karma-fn inc)
            "dec" (karma-fn dec)
            "identity" print-karma)
          (merge com-m {:args [snick]}))))))
  (:cmd
   "Checks the karma of the person you specify."
   #{"karma" "identity"} print-karma)
  (:cmd
   "Increases the karma of the person you specify."
   #{"inc"} (karma-fn inc))
  (:cmd
   "Decreases the karma of the person you specify."
   #{"dec"} (karma-fn dec))
  (:indexes [[:server :channel :nick]]))
