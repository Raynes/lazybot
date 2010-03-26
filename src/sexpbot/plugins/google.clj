(ns sexpbot.plugins.google
  (:use (sexpbot commands respond)
	[clojure.contrib.duck-streams :only [slurp*]])
  (:require [org.danlarkin.json :as json]))

(defn google [term]
  (json/decode-from-str 
   (slurp* (str "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=" 
		(java.net.URLEncoder/encode term)))))

(defn cull [result-set]
  [(:estimatedResultCount (:cursor (:responseData result-set)))
   (first (:results (:responseData result-set)))])

(defmethod respond :google [{:keys [bot channel args]}]
  (let [[res-count res-map] (-> (apply str (interpose " " args)) google cull)
	title (:titleNoFormatting res-map)
	url (:url res-map)]
    (.sendMessage bot channel (str "First out of " res-count " results is:"))
    (.sendMessage bot channel title)
    (.sendMessage bot channel url)))

(defmodule :google {"google" :google})