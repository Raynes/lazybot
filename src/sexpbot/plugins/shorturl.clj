(ns sexpbot.plugins.shorturl
  (:use [sexpbot respond info]
	[clojure.contrib.io :only [slurp*]]
	[clj-config.core :only [get-key]])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http]
	    [irclj.irclj :as ircb])
  (:import java.net.URI))


(def bitkey (-> :bitly-key (get-key info-file)))
(def login (-> :bitly-login (get-key info-file)))

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

(defplugin
  (:short 
   "Shortens a URL with bit.ly"
   ["shorten" "short"] 
   [{:keys [irc channel nick args]}]
   (ircb/send-message irc channel (->> args first shorten-url (str nick ": ")))))