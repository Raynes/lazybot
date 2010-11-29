(ns sexpbot.plugins.shorturl
  (:use [sexpbot registry]
        [clojure.contrib.json :only [read-json]]
        [clojure-http.client :only [add-query-params]])
  (:require [clojure-http.resourcefully :as res])
  (:import java.net.URI))

(defn grab-url [js]
  (-> js :results vals first :shortUrl))

(defn is-gd [url]
  (-> (res/get (add-query-params "http://is.gd/api.php" {"longurl" url})) :body-seq first))
  
(defn bit-ly [url login bitkey]
  (grab-url (read-json 
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

(defn shorten [{:keys [bot channel nick args] :as com-m} site]
  (if-let [url (first args)]
    (send-message com-m (str nick ": " (shorten-url url bot site)))
    (send-message com-m "You didn't specify a URL!")))

(defplugin
  (:cmd
   "Gets a shortened URL from bit.ly"
   #{"bitly" "bit-ly" "bit.ly"}
   (fn [irc-map]
     (shorten irc-map "bitly")))
  
   (:cmd
    "Gets a shortened URL from isgd"
    #{"is-gd" "is.gd" "isgd"}
    (fn [irc-map]
      (shorten irc-map "isgd")))
   
   (:cmd
    "Gets a shortened URL from dottk"
    #{"dottk" ".tk" "dot-tk"}
    (fn [irc-map]
      (shorten irc-map "dottk"))))
		 
