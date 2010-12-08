(ns sexpbot.plugins.log
  (:use [sexpbot registry utilities]
        clojure.contrib.logging)
  (:import [org.apache.log4j Level]))

(defn set-log-level [package level]
  (let [package (if (not= (.indexOf package ".") -1)
                  package
                  (str "sexpbot.plugins." package))]
    (info (str "Setting log level for " package " to " level))
    (.setLevel (get-logger package) level)))

(defplugin
  (:cmd
   "Set the log level for a plugin"
   #{"log"}
   (fn [{:keys [bot nick com channel args] :as com-m}]
     (let [[level & pkgs] args
           level (Level/toLevel (.toUpperCase level))]
       (if-admin
        nick com-m bot
        (doseq [p pkgs]
          (set-log-level p level)))))))