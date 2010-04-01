(ns sexpbot.utilities
  (:require [org.danlarkin.json :as json])
  (:import (java.io File FileReader)))

(defn stringify [coll]
  (apply str (interpose " " coll)))

(defn if-exists-read [file]
  (into {} 
	(if (.exists (File. file))
	  (-> file slurp read-string)
	  nil)))