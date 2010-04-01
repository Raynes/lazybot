(ns sexpbot.plugins.translate
  (:use (sexpbot respond commands utilities)
	[clojure.contrib.duck-streams :only [slurp*]])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http])
  (:import (org.apache.commons.lang StringEscapeUtils)))

(defn translate [lang1 lang2 text]
  (-> (http/get 
       (java.net.URI. "http://ajax.googleapis.com/ajax/services/language/translate")
       :query {:v "1.0"
	       :q text
	       :langpair (str lang1 "|" lang2)} :as :string) :content json/decode-from-str))

(defmethod respond :translate [{:keys [bot channel args]}]
  (let [[lang-from lang-to & text] args
	translation (translate lang-from lang-to (stringify text))]
    (if (:responseData translation)
      (.sendMessage bot channel (-> translation 
				    :responseData 
				    :translatedText 
				    StringEscapeUtils/unescapeHtml))
      (.sendMessage bot channel "Languages not recognized."))))

(defmodule :translate 
  {"translate" :translate
   "trans"     :translate})