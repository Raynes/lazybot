(ns sexpbot.core
  (:use (sexpbot respond info)
	[clojure.stacktrace :only [root-cause]]
	[clojure.contrib.str-utils :only [re-split]])
  (:require [org.danlarkin.json :as json]
	    (sexpbot.plugins utils eball google lmgtfy translate 
			     eval whatis dynamic leet shorturl
			     dictionary brainfuck spellcheck weather))
  (:import (org.jibble.pircbot PircBot)
	   (java.io File FileReader)
	   (org.apache.commons.io FileUtils)
	   (java.util.concurrent FutureTask TimeUnit TimeoutException)))

(def prepend \$)
(def server "irc.freenode.net")
(def channels ["#()" "#clojure-casual"])

(defn wall-hack-method [class-name name- params obj & args]
  (-> class-name (.getDeclaredMethod (name name-) (into-array Class params))
    (doto (.setAccessible true))
    (.invoke obj (into-array Object args))))

(defn split-args [s] (let [[command & args] (re-split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

;;; Possible future privilege system ;;;
(defn get-priv [user]
  ((->> "/privileges.clj" 
	(str sexpdir) 
	FileReader. 
	json/decode-from-reader 
	(into {})) (keyword user)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn make-bot-obj []
  (proxy [PircBot] []
    (onMessage 
     [chan send login host mess]
     (let [bot-map {:bot this
		    :sender send
		    :channel chan
		    :login login
		    :host host}]
       (if (= (first mess) prepend)
	 (try
	  (thunk-timeout 
	   #(try
	     (-> bot-map (merge (split-args (apply str (rest mess)))) respond)
	     (catch Exception e 
	       (.sendMessage this chan (.getMessage (root-cause e)))))
	   11)
	  (catch TimeoutException _
	    (.sendMessage this chan "Execution Timed Out!"))))))))

(defn make-bot [] 
  (let [bot (make-bot-obj)]
    (wall-hack-method PircBot :setName [String] bot "sexpbot")
    (doto bot
      (.setVerbose true)
      (.connect server))
    (doseq [chan channels] (.joinChannel bot chan))))

(setup-info)
(make-bot)