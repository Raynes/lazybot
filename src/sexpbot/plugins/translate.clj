(ns sexpbot.plugins.translate
  (:use [sexpbot respond utilities]
	[clojure.contrib.io :only [slurp*]])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http]
	    [irclj.irclj :as ircb])
  (:import org.apache.commons.lang.StringEscapeUtils))

(defn translate [lang1 lang2 text]
  (-> (http/get 
       (java.net.URI. "http://ajax.googleapis.com/ajax/services/language/translate")
       :query {:v "1.0"
	       :q text
	       :langpair (str lang1 "|" lang2)} :as :string) :content json/decode-from-str))

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
					  StringEscapeUtils/unescapeHtml))
       (ircb/send-message irc channel "Languages not recognized.")))))