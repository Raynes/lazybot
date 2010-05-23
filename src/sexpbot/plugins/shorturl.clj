(ns sexpbot.plugins.shorturl
  (:use [sexpbot respond info]
	[clojure.contrib.io :only [slurp*]]
	[clj-config.core])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http]
	    [irclj.irclj :as ircb])
  (:import java.net.URI))


(def bitkey (-> :bitly-key (get-key info-file)))
(def login (-> :bitly-login (get-key info-file)))
(def prepend (:prepend (read-config info-file)))

(defn grab-url [js]
  (-> js :results vals first :shortUrl))

(defn is-gd [url]
 (:content
  (http/get (URI. "http://is.gd/api.php")
	    :query {:longurl url} :as :string)))
  
(defn bit-ly [url]
  (grab-url (json/decode-from-str 
	     (:content
	      (http/get (URI. "http://api.bit.ly/shorten") 
			:query {:login login 
				:apiKey bitkey
				:longUrl (apply str (cons (if-not (= (apply str (take 7 url)) "http://") "http://" "") url)) ;;prepend http://
				:version "2.0.1"} :as :string)))))

(defn dot-tk [url]
  (.substring (:content
	  (http/get (URI. "http://api.dot.tk/tweak/shorten")
		    :query {:long url} :as :string)) 0 16))

(defn shorten-url [url site]
  (cond
   (= site "bitly") (bit-ly url)
   (= site "isgd") (is-gd url)
   (= site "dottk") (dot-tk url)
   :else "Service is not supported"))

(defn shorten [{:keys [irc channel nick args]} site]
  (if-let [url (first args)]
    (ircb/send-message irc channel (str nick ": " (shorten-url url site)))
    (ircb/send-message irc channel "You didn't specify a URL!")))

(defplugin
  (:bit-ly
   "Gets a shortened URL from bit.ly"
   ["bitly" "bit-ly" "bit.ly"]
   [irc-map]
   (shorten irc-map "bitly"))
  
   (:is-gd
    "Gets a shortened URL from isgd"
    ["is-gd" "is.gd" "isgd"]
    [irc-map]
    (shorten irc-map "isgd"))
   
   (:dot-tk
    "Gets a shortened URL from dottk"
    ["dottk" ".tk" "dot-tk"]
    [irc-map]
    (shorten irc-map "dottk")))
		 
