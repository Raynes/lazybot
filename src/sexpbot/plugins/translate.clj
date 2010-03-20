(ns sexpbot.plugins.translate
  (:use (sexpbot respond commands utilities)
	[clojure.contrib.duck-streams :only [slurp*]])
  (:require [org.danlarkin.json :as json])
  (:import (org.apache.commons.lang StringEscapeUtils)))

(def translate-cmds {"translate" :translate})

(defn translate [lang1 lang2 text]
  (json/decode-from-str 
   (slurp* (str 
	    "http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&q=" 
	    (java.net.URLEncoder/encode text)
	    "&langpair="
	    (java.net.URLEncoder/encode (str lang1 "|" lang2))))))

(defmethod respond :translate [{:keys [bot channel args]}]
  (let [[lang-from lang-to & text] args
	translation (translate lang-from lang-to (stringify text))]
    (if (:responseData translation)
      (.sendMessage bot channel (-> translation 
				    :responseData 
				    :translatedText 
				    StringEscapeUtils/unescapeHtml))
      (.sendMessage bot channel "Languages not recognized."))))

(defmodule translate-cmds :translate)