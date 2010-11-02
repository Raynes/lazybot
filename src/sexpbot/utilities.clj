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

(defn predicated-swapfn
  "Create a function suitable for swap! that returns either the result
of (f val args), or val if f returns a value that does not satisfy
pred. If specified, a should be an atom; it will be set to nil if the
swap changed, otherwise [new value]."
  ([f pred]
     (predicated-swapfn f pred (atom nil)))
  ([f pred a]
     (fn [val & args]
       (let [new (apply f val args)
	     changed (pred new)]
	 (reset! a (when changed [new]))
	 (if changed
	   new
	   val)))))

(defn capped-inc!
  "Destructively increase the integer stored in atom, but don't let it get
bigger than cap."
  [ref cap]
  (let [modified (atom nil)]
    (swap! ref (predicated-swapfn inc #(<= % cap) modified))
    (first @modified)))

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