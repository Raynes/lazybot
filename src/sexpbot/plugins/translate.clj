(ns sexpbot.plugins.translate
  (:use [sexpbot respond utilities])
  (:require [org.danlarkin.json :as json]
	    [clojure-http.resourcefully :as res]
	    [irclj.irclj :as ircb])
  (:import org.apache.commons.lang.StringEscapeUtils))

(defn translate [lang1 lang2 text]
  (-> (res/get 
       "http://ajax.googleapis.com/ajax/services/language/translate"
       {} {"v" "1.0" "q" text "langpair" (str lang1 "|" lang2)})
      :body-seq first json/decode-from-str))

(defplugin
  (:translate
   "Translates with google translate. Takes two language abbreviations (google's ones) and some text
   to translate, and returns it translated."
   ["trans" "translate"]
   [{:keys [irc channel args]}]
   (let [[lang-from lang-to & text] args
	 translation (translate lang-from lang-to (stringify text))]
     (if (:responseData translation)
       (ircb/send-message irc channel (-> translation 
					  :responseData 
					  :translatedText 
					  StringEscapeUtils/unescapeHtml
					  (.replaceAll "\n|\r" "")))
       (ircb/send-message irc channel "Languages not recognized.")))))