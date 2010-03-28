(ns sexpbot.utilities
  (:require [org.danlarkin.json :as json])
  (:import (java.io File FileReader)))

(defn stringify [coll]
  (apply str (interpose " " coll)))

(defn if-exists-decode [file]
  (into {} 
	(if (.exists (File. file))
	  (json/decode-from-reader (FileReader. file))
	  nil)))