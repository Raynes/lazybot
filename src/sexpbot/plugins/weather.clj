(ns sexpbot.plugins.weather
  (:use (sexpbot commands respond))
  (:require [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf]))

(def forcasturl "http://api.wunderground.com/auto/wui/geo/ForecastXML/index.xml?query=")

(defn cull [zipper]
  [(first (zf/xml-> zipper :txt_forecast :date zf/text)) 
   (zf/xml-> zipper :txt_forecast :forecastday :fcttext zf/text)])

(defn get-fcst [query]
  (->> query (remove #(= \space %)) (apply str) (str forcasturl) xml/parse zip/xml-zip cull))

(defmethod respond :fcst [{:keys [bot channel sender args]}]
  (let [[date [today tonight]] (->> args (remove #(= \space %)) get-fcst)]
    (if (seq date)
      (do
	(.sendMessage bot channel (str sender ": " date))
	(.sendMessage bot channel (str sender ": TODAY: " today))
	(.sendMessage bot channel (str sender ": TONIGHT: " tonight)))
      (.sendMessage bot channel (str sender ": Location not found!")))))

(defmodule :weather
  {"fcst" :fcst})