(ns sexpbot.plugins.weather
  (:use [sexpbot respond]
	[clojure.contrib.str-utils :only [re-sub]])
  (:require [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf]
	    ))

(def forcasturl "http://api.wunderground.com/auto/wui/geo/ForecastXML/index.xml?query=")

(defn cull [zipper]
  (let [[date & more] (zf/xml-> zipper :txt_forecast :date zf/text)
	[pdate & more] (zf/xml-> zipper :simpleforecast :forecastday :date :pretty_short zf/text)
	ftext (zf/xml-> zipper :txt_forecast :forecastday :fcttext zf/text)
	[hi & more] (zf/xml-> zipper :simpleforecast :forecastday :high :celsius zf/text)
	[low & more] (zf/xml-> zipper :simpleforecast :forecastday :low :celsius zf/text)
	[condition & more] (zf/xml-> zipper :simpleforecast :forecastday :conditions zf/text)]
    (if (seq date)
      [date ftext]
      [pdate (str "High: " hi " Low: " low " Conditions: " condition)])))

(defn strip-space [s]
  (re-sub #", " "," (apply str (seq s))))

(defn get-fcst [query]
  (->> query
       strip-space
       (#(.replace % " " "%20"))
       (str forcasturl)
       xml/parse 
       zip/xml-zip 
       cull))

(defplugin
  (:fcst 
   "Get's the forecast for a location. Can take a zipcode, or a City, State combination."
   ["fcst"]
   [{:keys [irc channel nick args]}]
   (let [[date [today tonight :as a]] (->> args (interpose " ") get-fcst)
	 conditions (if (string? today) today a)]
     (if (seq date)
       (do
	 (send-message irc channel (str nick ": " date))
	 (send-message irc channel (str nick ": TODAY: " conditions))
	 (when (string? today)
	   (send-message irc channel (str nick ": TONIGHT: " tonight))))
       (send-message irc channel (str nick ": Location not found!"))))))

