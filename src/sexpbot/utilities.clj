(ns sexpbot.utilities
  (:use sexpbot.info)
  (:require [clojure.contrib.io :as io]
            [clojure.string :only [join] :as string])
  (:import [java.io File FileReader]
	   [java.util.concurrent FutureTask TimeUnit TimeoutException]))

(defn stringify [coll]
  (apply str (interpose " " coll)))

(defn if-exists-read [file]
  (into {} 
	(if (.exists (File. file))
	  (-> file slurp read-string)
	  nil)))

(defmacro keywordize [vars]
  (into {} (map (juxt keyword identity)
                vars)))

;; This is a bit ugly. Each entry in the table describes how many of the
;; labelled unit it takes to constitute the next-largest unit. It can't be
;; a map because order matters.
(def time-units
     [['millisecond 1000]
      ['second 60]
      ['minute 60]
      ['hour 24]
      ['day 7]
      ['week Integer/MAX_VALUE]]) ; Extend if you want month/year/whatever

(defn decorate [num label]
  (when (> num 0)
    (str num " " label (if (> num 1) "s" ""))))

;; Run through each time unit, finding mod and quotient of the time left.
;; Track the number of minutes left to work on, and the list of decorated values.
;; Note that we build it back-to-front so that larger units end up on the left.
(defn compute-units [ms]
  (second
   (reduce (fn [[time-left units-so-far] [name ratio]]
             (let [[time-left r] ((juxt quot rem) time-left ratio)]
               [time-left (cons (decorate r name)
                                units-so-far)]))
           [ms ()] ; Start with no labels, and all the time
           time-units)))

;; Now drop out the nil labels, and glue it all together
(defn format-time [ms]
  (when-not (= ms 0)
    (->> (compute-units ms)
         (drop-while nil?)
         (take 2) ; If a high-order thing like week is nonzero, don't bother with hours
         (remove nil?)
         (string/join " and "))))

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