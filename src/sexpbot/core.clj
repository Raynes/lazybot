(ns sexpbot.core
  (:use (sexpbot.plugins utils eball google lmgtfy translate 
			 eval whatis dynamic leet)
	sexpbot.respond
	[clojure.contrib.str-utils :only [re-split]])
  (:require [org.danlarkin.json :as json])
  (:import (org.jibble.pircbot PircBot)
	   (java.io File FileReader)))

(def prepend \$)
(def server "irc.freenode.net")
(def channels ["#()" "#clojure-casual"])
(def sexpdir (File. (str (System/getProperty "user.home") "/.sexpbot" )))

(defn wall-hack-method [class-name name- params obj & args]
  (-> class-name (.getDeclaredMethod (name name-) (into-array Class params))
    (doto (.setAccessible true))
    (.invoke obj (into-array Object args))))

(defn split-args [s] (let [[command & args] (re-split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

(when (not (.exists sexpdir)) (.mkdir sexpdir))
(.createNewFile (File. (str (str sexpdir) "/privileges.clj")))

;;; Possible future privilege system ;;;
(defn get-priv [user]
  ((->> "/privileges.clj" 
	(str sexpdir) 
	FileReader. 
	json/decode-from-reader 
	(into {})) (keyword user)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-bot [] 
  (let [bot (proxy [PircBot] []
	      (onMessage 
	       [chan send login host mess]
	       (let [bot-map
		     {:bot this
		      :sender send
		      :channel chan
		      :login login
		      :host host}]
		 (if (= (first mess) prepend)
		   (respond (merge (split-args (apply str (rest mess))) bot-map))))))]
    (wall-hack-method PircBot :setName [String] bot "sexpbot")
    (doto bot
      (.setVerbose true)
      (.connect server))
    (doseq [chan channels] (.joinChannel bot chan))))

(make-bot)