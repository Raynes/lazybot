(ns lazybot.plugins.buildbot
  (:import (java.util Date))
  (:use lazybot.registry)
  (:require [lazybot.buildbot.cctray :as cctray]
            [clj-http.client :as http]
            [clojure.pprint :refer [pprint]]))

(def cctray-url "http://dc1dev01:8153/go/cctray.xml")

(defn get-project-statuses []
  (println "getting project data")
  (try
    (let [{:keys [status body] :as response} (http/get cctray-url)]
      (if (= status 200)
        (cctray/projects body)
        (do
          (println "invalid http response:" response)
          nil)))
    (catch Exception e
      (println "Caught exception " e)
      nil)))

(defn report [com-m events]
  ;  (clojure.pprint/pprint events)
  (doseq [event events]
    (do
      (println "reporting: " event)
      (send-message com-m (:message event)))))

(defn now-ms []
  (.getTime (Date.)))

(def last-status (atom nil))

(defn check-builds [com-m verbose]
  (try
    (if-let [status (get-project-statuses)]
      (do
        (if-not @last-status
          (when verbose (send-message com-m "initial build status checked - run 'check-builds' again to look for changes"))
          (let [events (cctray/status->events (:status @last-status) status)]
            (if (empty? events)
              (when verbose (send-message com-m "no change in builds since last check"))
              (report com-m events))))
        (reset! last-status {:status status :time (now-ms)})
      ))
  (catch Exception e
    (do
      (when verbose (send-message com-m (str "something went wrong:" e)))
      (println "caught exception:" e)
      (clojure.stacktrace/print-stack-trace e)
      ))))

(defn auto-check [{:keys [channel] :as com-m}]
  (println "channel:" channel)
  (if (contains? #{"#general" "lazybot"} channel)
  (if-not @last-status
    (check-builds com-m true) ; initial check
    (let [then (:time @last-status)
           now (now-ms)
          _ (prn then now (- now then))]
      (if (> (- now then) 5000)
        (do
          (println "checking")
          (check-builds com-m false)
          )))
    )))

(defplugin
  (:cmd "check build status"
   #{"check-builds"}
   (fn [m] (check-builds m true)))

  (:cmd "reset build status"
   #{"reset-builds"}
   (fn [com-m] (reset! last-status nil)
     (send-message com-m "reset.")))

  (:cmd "get all statuses"
   #{"statuses"}
   (fn [m] (let [statuses (get-project-statuses)
                 counts (frequencies (map :lastBuildStatus (vals statuses)))
                 _ (send-message m "Project build statuses")]
             (doseq [[status count] counts]
               (send-message m (str "Status:\t\"" status "\"\t:\t" count)))
             )))
  (:cmd
   "get failed projects"
   #{"failures"}
   (fn [com-m]
     (do
       (send-message com-m "Projects currently failed:")
       (let [statuses (get-project-statuses)
             failures (filter #(= (:lastBuildStatus %) "Failure") (vals statuses))
             names (sort (map :name failures))]
         (doseq [name names]
           (send-message com-m name)))
       (send-message com-m "that's all!"))))

  (:hook
   :on-any
   auto-check)
  )