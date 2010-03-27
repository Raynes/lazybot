(ns sexpbot.plugins.whatis
  (:use [sexpbot respond commands])
  (:require [org.danlarkin.json :as json])
  (:import (java.io FileWriter FileReader File)))

(def whatis (str (System/getProperty "user.home") "/.sexpbot/whatis.json"))

(defmethod respond :put [{:keys [bot channel args]}]
  (let [[subject & is] args
	current (if (.exists (File. whatis)) 
		  (seq (json/decode-from-reader (FileReader. whatis)))
		  nil)]
    (.flush (json/encode-to-writer 
	     (merge (into {} current) 
		    {(keyword subject) (apply str (interpose " " is))}) 
	     (FileWriter. whatis)))
    (.sendMessage bot channel (str subject " = " (apply str (interpose " " is))))))

(defmethod respond :get [{:keys [bot channel args]}]
  (let [whatmap (if (.exists (File. whatis))
		  (seq (json/decode-from-reader (FileReader. whatis)))
		  nil)
	result (->> (first args) keyword ((into {} whatmap)))]
    (if result
      (.sendMessage bot channel (str (first args) " = " result)))))

(defmodule :whatis
  {"put" :put
   "get" :get})