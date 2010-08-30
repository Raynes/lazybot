(ns sexpbot.plugins.shorturl
  (:use [sexpbot respond ]
        [clojure-http.client :only [add-query-params]])
  (:require [org.danlarkin.json :as json]
	    [clojure-http.resourcefully :as res])
  (:import java.net.URI))

(defn grab-url [js]
  (-> js :results vals first :shortUrl))

(defn is-gd [url]
  (-> (res/get (add-query-params "http://is.gd/api.php" {"longurl" url})) :body-seq first))
  
(defn bit-ly [url login bitkey]
  (grab-url (json/decode-from-str 
	     (->> (res/get (add-query-params "http://api.bit.ly/shorten"
					     {"login" login 
                                              "apiKey" bitkey
                                              "longUrl" (if (.startsWith url "http://") url (str "http://" url))
                                              "version" "2.0.1"}))
		  :body-seq
		  (apply str)))))

(defn dot-tk [url]
  (.substring
   (->> (res/get (add-query-params "http://api.dot.tk/tweak/shorten" {"long" url}))
	:body-seq (apply str))
   0 15))

(defn shorten-url [url bot site]
  (cond
   (= site "bitly") (bit-ly url (:bitly-login (:config @bot)) (:bitly-key (:config @bot)))
   (= site "isgd") (is-gd url)
   (= site "dottk") (dot-tk url)
   :else "Service is not supported"))

(defn shorten [{:keys [irc bot channel nick args]} site]
  (if-let [url (first args)]
    (send-message irc bot channel (str nick ": " (shorten-url url bot site)))
    (send-message irc bot channel "You didn't specify a URL!")))

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
		 
