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
				:longUrl url
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

(defplugin
  (:short 
   "Interal method, use bit-ly or is-gd instead"
   ["shorten*"] 
   [{:keys [irc channel nick args]}]
   (let [url (first args)
	 site (second args)]
     (ircb/send-message irc channel (str nick ": " (shorten-url url site)))))
  
  (:bit-ly
   "Gets a shortened URL from bit.ly"
   ["bitly" "bit-ly" "bit.ly"]
   [{:keys [irc channel nick args] :as irc-map}]
   (if-let [url (first args)]
     (try-handle (assoc irc-map :message (str prepend "shorten* " url " bitly")))
     (ircb/send-message irc channel "You didn't specify a URL!")))
  
   (:is-gd
    "Gets a shortened URL from isgd"
    ["is-gd" "is.gd" "isgd"]
    [{:keys [irc channel nick args] :as irc-map}]
    (if-let [url (first args)]
      (try-handle (assoc irc-map :message (str prepend "shorten* " url " isgd")))
      (ircb/send-message irc channel "You didn't specify a URL!")))
   
   (:dot-tk
    "Gets a shortened URL from dottk"
    ["dottk" ".tk" "dot-tk"]
    [{:keys [irc channel nick args] :as irc-map}]
    (if-let [url (first args)]
      (try-handle (assoc irc-map :message (str prepend "shorten* " url " dottk")))
      (ircb/send-message irc channel "You didn't specify a URL!"))))
		 
