(ns sexpbot.plugins.dictionary
  (:use [sexpbot respond]
	[clojure-http.client :only [add-query-params]]
        [clojure.contrib.json :only [read-json]])
  (:require [clojure-http.resourcefully :as res])
  (:import java.net.URI))

(defn extract-stuff [js]
  (let [text (:text js)]
    [(.replaceAll (if (seq text) text "") "\\<.*?\\>" "") (:partOfSpeech js)]))

(defn lookup-def [key word]
  (-> (res/get
       (add-query-params (str "http://api.wordnik.com/api/word.json/" word "/definitions") {"count" "1"})
       {"api_key" key})
      :body-seq first read-json first extract-stuff))

(defplugin 
  (:dict 
   "Takes a word and look's up it's definition via the Wordnik dictionary API." 
   ["dict"] 
   [{:keys [irc bot channel nick args]}]
   (send-message irc bot channel 
		      (str nick ": " 
			   (let [[text part] (lookup-def (:wordnik-key (:config @bot)) (first args))]
			     (if (seq text) (str part ": " text) "Word not found."))))))