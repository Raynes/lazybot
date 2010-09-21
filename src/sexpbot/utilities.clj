(ns sexpbot.utilities
  (:use sexpbot.info)
  (:require [clojure.contrib.io :as io])
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