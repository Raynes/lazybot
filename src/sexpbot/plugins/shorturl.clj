(ns sexpbot.plugins.shorturl
  (:use [sexpbot respond info]
	[clojure.contrib.duck-streams :only [slurp*]])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http]
	    [irclj.irclj :as ircb])
  (:import java.net.URI))


(def bitkey (-> :bitly-key get-key))
(def login (-> :bitly-login get-key))

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

(defmethod respond :short [{:keys [irc channel nick args]}]
  (ircb/send-message irc channel (->> args first shorten-url (str nick ": "))))

(defplugin
  {"short"   :short
   "shorten" :short})