(ns sexpbot.plugins.dictionary
  (:use (sexpbot respond info))
  (:require [com.twinql.clojure.http :as http]
	    [org.danlarkin.json :as json])
  (:import (java.net URI)
	   org.apache.commons.lang.StringEscapeUtils))

(def wordnik-key (-> :wordnik-key get-key))

(defn extract-stuff [js]
  (let [text (:text js)]
    [(.replaceAll (if (seq text) text "") "\\<.*?\\>" "") (:partOfSpeech js)]))

(defn lookup-def [word]
  (-> (http/get 
       (URI. (str "http://api.wordnik.com/api/word.json/" word "/definitions"))
       :query {:count "1"}
       :headers {"api_key" wordnik-key}
       :as :string)
      :content json/decode-from-str first extract-stuff))

(defmethod respond :dict [{:keys [bot channel sender args]}]
  (.sendMessage bot channel 
		(str sender ": " 
		     (let [[text part] (lookup-def (first args))]
		       (if (seq text) (str part ": " text) "Word not found.")))))

(defmodule :dictionary
  {"dict" :dict})