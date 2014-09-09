(ns lazybot.buildbot.cctray
  (:require [clojure.data.xml :refer [parse-str]]
            [clojure.zip :refer [xml-zip up down left right]]
            [clojure.data.zip :as c-d-zip]
            [clojure.data.zip.xml
             :refer [xml-> xml1-> attr attr= text]]
            [clojure.java.io :as io]
            [lazybot.buildbot.xmlutil :refer :all]
            [clojure.core.match :refer [match]]
            ))


(defn z [data] (xml-zip (parse-str data))) ; for convenience

(defn z->map [p]
  (let [n (clojure.zip/node p)
        attrs (:attrs n)
        tags (map :tag (:content n))
        breakers (xml-> p :messages :message (attr= :kind "Breakers") (attr :text))]
    (when (not= (count tags) (count breakers))
      (println "warning: some content not understood in " (:content n)))
    (when (> (count breakers) 1)
      (println "warning: multiple Breakers found, ignoring all but first"))
    (if (empty? breakers)
      attrs
      (merge attrs {:breakers (first breakers)}))))

(defn projects [xml-data]
  (into {}
        (for [projdata (xml-> (z xml-data)
                              :Project
                              z->map)]
          [(:name projdata) projdata])))

; name
; activity (def activities #{"Sleeping" "Building" "CheckingModifications"})
; lastBuildStatus (def build-statuses #{"Success" "Failure" "Exception" "Unknown"})

; :lastBuildLabel is a string (actually a number) - optional
; :lastBuildTime is an ISO DateTime
; :nextBuildTime is an ISO DateTime - optional
; :webUrl is a url

(defn removed-event [project]
  {:event :removed
   :project (:name project)
   :message (str (:name project) " - game over.")})

(defn added-event [project]
  {:event :added
   :project (:name project)
   :message (str "Player " (:name project) " enters")})

(defn change-event [kind project message]
  (let [br-message (if (:breakers project)
                     (str message " - probably caused by " (:breakers project))
                     message)]
    {:event kind
     :project (:name project)
     :message br-message}))

(defn status-change
  [{s1 :lastBuildStatus :as p1}
   {s2 :lastBuildStatus name :name :as p2}]
  (if (not= s1 s2)
    (match [s1 s2]
           [_ "Success"] (change-event :fixed p2 (str name " is fixed!"))
           [_ "Unknown"] (change-event :unknown p2 (str name " is unknown - something strange happened"))
           [_ "Exception"] (change-event :broken p2 (str name " threw an exception!"))
           ["Success" "Failure"] (change-event :broken p2 (str name " is broken!"))
           ["Exception" "Failure"] (change-event :broken p2 (str name " was exceptional - now it's just broken"))
           ["Unknown" "Failure"] (change-event :broken p2 (str name " is alive again, but still broken!"))
           :else (println "something broke processing status-change for " p1 p2))))

(defn changed-events [project1 project2]
  (when-not (= project1 project2)
    (status-change project1 project2)))

(defn status->events [p1 p2]
  (let [k1 (set (keys p1))
        k2 (set (keys p2))
        common (clojure.set/intersection k1 k2)
        removed (clojure.set/difference k1 k2)
        added (clojure.set/difference k2 k1)]
    (filter #(not (nil? %))
            (concat
              (map #(removed-event (get p1 %)) removed)
              (map #(added-event (get p2 %)) added)
              (map #(changed-events (get p1 %) (get p2 %)) common)))))

(defn sample []
  (let [file1 "src/buildbot/cctray.xml"
        file2 "src/buildbot/cctray2.xml"
        p1 (projects (slurp file1))
        p2 (projects (slurp file2))]
    (clojure.pprint/pprint (status->events p1 p2))))


