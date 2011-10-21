(ns lazybot.utilities
  (:use lazybot.info
        [hobbit core isgd])
  (:require [clojure.string :only [join] :as string])
  (:import [java.io File FileReader]
           [org.apache.log4j LogManager]))

(defn if-exists-read [file]
  (into {}
        (when (.exists (File. file))
          (-> file slurp read-string))))

(defn shorten-url
  "Shorten a URL using is.gd."
  [url] (shorten (shortener :idgd) url))

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

(defn pluralize [num label]
  (when (> num 0)
    (str num " " label (if (> num 1) "s" ""))))

;; Run through each time unit, finding mod and quotient of the time left.
;; Track the number of minutes left to work on, and the list of labeled values.
;; Note that we build it back-to-front so that larger units end up on the left.
(defn compute-units [ms]
  (second
   (reduce (fn [[time-left units-so-far] [name ratio]]
             (let [[time-left r] ((juxt quot rem) time-left ratio)]
               [time-left (cons (pluralize r name)
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

(defn split-str-at [len s]
  (map #(apply str %)
       ((juxt take drop) len s)))

(defn verify
  "Return x, unless (pred x) is logical false, in which case return
nil."
  [pred x]
  (when (pred x)
    x))

(defn validator
  [pred]
  (partial verify pred))

(defn trim-string
  "Trim the specified string down to a maximum length. If any trimming needs
to be done, then trim-indication-generator will be called with s as its argument
to create an indication that trimming has been done; the resulting string will
be added to the end of the string, trimming again to fit into the maximum size."
  [max-len trim-indication-generator s]
  (let [[before after] (split-str-at max-len s)]
    (if-not (seq after)
      before
      (let [indicator (trim-indication-generator s)]
        (str (.substring s 0 (- max-len (count indicator)))
             indicator)))))
