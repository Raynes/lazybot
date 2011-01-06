(ns sexpbot.utilities
  (:use sexpbot.info)
  (:require [clojure.contrib.io :as io]
            [clojure.string :only [join] :as string])
  (:import [java.io File FileReader]
           [org.apache.log4j LogManager]))

;; support legacy code, written before (join) was invented
(def stringify string/join)

(defn if-exists-read [file]
  (into {} 
	(when (.exists (File. file))
	  (-> file slurp read-string))))

(defmacro keywordize
  "Create a map in which, for each symbol S in vars, (keyword S) is a
  key mapping to the value of S in the current scope."
  [vars]
  (into {} (map (juxt keyword identity)
                vars)))

(defn verify
  "Return x, unless (pred x) is logical false, in which case return nil."
  [pred x]
  (when (pred x)
    x))

(defn transform-if
  "Returns a function that tests pred against its argument. If the result
is true, return (f arg); otherwise, return (f-not arg) (defaults to
identity)."
  ([pred f]
     (fn [x]
       (if (pred x) (f x) x)))
  ([pred f f-not]
     (fn [x]
       (if (pred x) (f x) (f-not x)))))

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

(defmacro on-thread
  "Run the body in an anonymous, new thread. Very much like
  clojure.core/future, but with Java's default error-handling
  semantics instead of those of (future)."
  [& body]
  `(.start (Thread. (fn [] ~@body))))

(defn get-logger
  ([] (get-logger (str *ns*)))
  ([ns]
     (LogManager/getLogger (str ns))))
