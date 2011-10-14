(ns lazybot.plugins.shorturl
  (:use [lazybot registry]
        [clojure.data.json :only [read-json]])
  (:require [clj-http.client :as http])
  (:import java.net.URI))

(defn grab-url [js]
  (prn js)
  (-> js :results vals first :shortUrl))

(defn is-gd [url]
  (:body
   (http/get "http://is.gd/api.php"
             {:query-params {"longurl" url}})))
  
(defn bit-ly [url login bitkey]
  (grab-url (read-json 
	     (->> (http/get
                   "http://api.bit.ly/shorten"
                   {:query-params {"login" login 
                                   "apiKey" bitkey
                                   "longUrl" (if (.startsWith url "http://") url (str "http://" url))
                                   "version" "2.0.1"}})
		  :body))))

(defn dot-tk [url]
  (.substring
   (:body (http/get "http://api.dot.tk/tweak/shorten" {:query-params {"long" url}}))
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
		 
