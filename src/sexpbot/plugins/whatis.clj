(ns sexpbot.plugins.whatis
  (:use [sexpbot respond commands])
  (:require [org.danlarkin.json :as json])
  (:import (java.io FileWriter FileReader File)))

(def whatis (str (System/getProperty "user.home") "/.sexpbot/whatis.json"))

(defn if-exists-decode [file]
  (into {} 
	(if (.exists (File. file))
	  (json/decode-from-reader (FileReader. file))
	  nil)))

(defmethod respond :learn [{:keys [bot channel args]}]
  (let [[subject & is] args
	current (if-exists-decode whatis)]
    (.flush (json/encode-to-writer 
	     (merge current 
		    {subject (apply str (interpose " " is))}) 
	     (FileWriter. whatis) :indent 2))
    (.sendMessage bot channel "Never shall I forget it.")))

(defmethod respond :whatis [{:keys [bot channel args]}]
  (let [whatmap (if-exists-decode whatis)
	result (->> (first args) keyword whatmap)]
    (if result
      (.sendMessage bot channel (str (first args) " = " result))
      (.sendMessage bot channel (str (first args) " does not exist in my database.")))))

(defmethod respond :forget [{:keys [bot channel args]}]
  (let [whatmap (if-exists-decode whatis)
	subject (keyword (first args))]
    (if (whatmap subject) 
      (do (json/encode-to-writer (dissoc whatmap subject) (FileWriter. whatis) :indent 2)
	  (.sendMessage bot channel (str subject " is removed. RIP.")))
      (.sendMessage bot channel (str subject " is not in my database.")))))

(defmodule :whatis
  {"learn"  :learn
   "whatis" :whatis
   "forget" :forget})