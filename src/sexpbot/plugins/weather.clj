(ns sexpbot.plugins.weather
  (:use [sexpbot respond]
	[clojure.contrib.str-utils :only [re-sub]])
  (:require [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf])
  (:import [org.apache.commons.lang StringEscapeUtils]))

(def forecasturl "http://api.wunderground.com/auto/wui/geo/ForecastXML/index.xml?query=")

(defn cull [zipper]
  (let [date (zf/xml1-> zipper :txt_forecast :date zf/text)
        pdate (zf/xml1-> zipper :simpleforecast :forecastday :date :pretty_short zf/text)
        ftext (zf/xml-> zipper :txt_forecast :forecastday :fcttext zf/text)
        hi (zf/xml1-> zipper :simpleforecast :forecastday :high :celsius zf/text)
        low (zf/xml1-> zipper :simpleforecast :forecastday :low :celsius zf/text)
        condition (zf/xml1-> zipper :simpleforecast :forecastday :conditions zf/text)]
    (if (seq date)
      [date ftext]
      [pdate (str "High: " hi " Low: " low " Conditions: " condition)])))

(defn strip-space [s]
  (re-sub #", " "," (apply str (seq s))))

(defn unescape [str]
  (if-not (coll? str)
    (StringEscapeUtils/unescapeHtml str)
    (map unescape str)))

(defn get-fcst [query]
  (-> query
      strip-space
      (.replace " " "%20")
      (->> (str forecasturl))
      xml/parse
      zip/xml-zip 
      cull
      unescape))

(defplugin
  (:cmd
   "Gets the forecast for a location. Can take a zipcode, or a City, State combination."
   #{"fcst"}
   (fn [{:keys [irc bot channel nick args]}]
     (let [[date [today tonight :as a]] (->> args (interpose " ") get-fcst)
           conditions (if (string? today) today a)]
       (if (seq date)
         (do
           (send-message irc bot channel (str nick ": " date))
           (send-message irc bot channel (str nick ": TODAY: " conditions))
           (when (string? today)
             (send-message irc bot channel (str nick ": TONIGHT: " tonight))))
         (send-message irc bot channel (str nick ": Location not found!")))))))

