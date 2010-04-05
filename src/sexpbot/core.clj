(ns sexpbot.core
  (:use (sexpbot respond info privileges)
	[clojure.stacktrace :only [root-cause]]
	[clojure.contrib.str-utils :only [re-split]])
  (:require [org.danlarkin.json :as json]
	    (sexpbot.plugins utils eball google lmgtfy translate 
			     eval whatis dynamic leet shorturl
			     dictionary brainfuck spellcheck weather
			     login))
  (:import (org.jibble.pircbot PircBot)
	   (java.io File FileReader)
	   (java.util.concurrent FutureTask TimeUnit TimeoutException)))

(let [info (read-config)]
  (def prepend (:prepend info))
  (def server (:server info))
  (def channels (:channels info))
  (def plugins (:plugins info)))

(defn wall-hack-method [class-name name- params obj & args]
  (-> class-name (.getDeclaredMethod (name name-) (into-array Class params))
    (doto (.setAccessible true))
    (.invoke obj (into-array Object args))))

(defn split-args [s] (let [[command & args] (re-split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

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

(defn handle-message [chan send login host mess this]
  (let [bot-map {:bot this
		 :sender send
		 :channel chan
		 :login login
		 :host host
		 :privs (get-priv send)}]
    (if (= (first mess) prepend)
      (try
       (thunk-timeout 
	#(try
	  (-> bot-map (merge (->> mess rest (apply str) split-args)) respond)
	  (catch Exception e 
	    (.sendMessage this chan (.getMessage (root-cause e)))))
	11)
       (catch TimeoutException _
	 (.sendMessage this chan "Execution Timed Out!"))))))

(defn make-bot-obj []
  (proxy [PircBot] []
    (onMessage 
     [chan send login host mess] (handle-message chan send login host mess this))
    (onPrivateMessage
     [send login host message] (handle-message send send login host message this))
    (onQuit
     [send login host message] (when (find-ns 'sexpbot.plugins.login) 
				 (handle-message send send login host "quit" this)))))

(defn make-bot [] 
  (let [bot (make-bot-obj)
	pass (-> :bot-password ((read-config)))]
    (wall-hack-method PircBot :setName [String] bot "sexpbot")
    (doto bot
      (.setVerbose true)
      (.connect server))
    (when (seq pass) (.sendMessage bot "NickServ" pass))
    (doseq [chan channels] (.joinChannel bot chan))
    (doseq [plug plugins] (loadmod plug))))

(make-bot)