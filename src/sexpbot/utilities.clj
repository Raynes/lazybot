(ns sexpbot.utilities
  (:use sexpbot.info
	stupiddb.core)
  (:require [org.danlarkin.json :as json]
	    [clojure.contrib.io :as io])
  (:import [java.io File FileReader]
	   [java.util.concurrent FutureTask TimeUnit TimeoutException]))

(defn stringify [coll]
  (apply str (interpose " " coll)))

(defn if-exists-read [file]
  (into {} 
	(if (.exists (File. file))
	  (-> file slurp read-string)
	  nil)))

;;;;;; From clojurebot's sandbox.clj, adapted for my code. ;;;;;;
(defn thunk-timeout [thunk seconds]
      (let [task (FutureTask. thunk)
            thr (Thread. task)]
        (try
          (.start thr)
          (.get task seconds TimeUnit/SECONDS)
          (catch TimeoutException e
                 (.cancel task true)
                 (.stop thr (Exception. "Thread stopped!")) 
		 (throw (TimeoutException. "Execution Timed Out"))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn flush-db [db]
  (dosync
   (println (:file @db))
   (with-open [w (io/writer (:file @db))]
     (binding [*out* w]
              (prn (:data @db))))
      (.close (:log @db))
         (alter db assoc :log (io/writer (str (:file @db)
                                               ".log")))))

(defn db-close [db]
  "Closes a db, stops the auto saving and writes the entire log into the db file for faster startup."
  (.stop (:thread @db))
  (flush-db db)
  (.close (:log @db)))