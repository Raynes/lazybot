(ns sexpbot.plugins.shorturl
  (:use (sexpbot commands respond)
	[clojure.contrib.duck-streams :only [slurp*]])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http])
  (:import (java.net URI)))


(def bitkey "R_4e19aa9812e390a8d4079f03bdf255f5")
(def login "raynes")

(defn grab-url [js]
  (-> js :results vals first :shortUrl))

(defn shorten-url [url]
  (grab-url (json/decode-from-str 
	     (:content
	      (http/get (URI. "http://api.bit.ly/shorten") 
			:query {:login login 
				:apiKey bitkey
				:longUrl url
				:version "2.0.1"} :as :string)))))

(defmethod respond :short [{:keys [bot channel sender args]}]
  (.sendMessage bot channel (->> args first shorten-url (str sender ": "))))

(defmodule :shorturl
  {"short" :short
   "shorten" :short})