(ns sexpbot.plugins.dictionary
  (:use [sexpbot respond info]
	[clj-config.core :only [get-key]]
	[clojure-http.client :only [add-query-params]])
  (:require [clojure-http.resourcefully :as res]
	    [org.danlarkin.json :as json]
	    [irclj.irclj :as ircb])
  (:import java.net.URI))

(def wordnik-key (get-key :wordnik-key info-file))

(defn extract-stuff [js]
  (let [text (:text js)]
    [(.replaceAll (if (seq text) text "") "\\<.*?\\>" "") (:partOfSpeech js)]))

(defn lookup-def [word]
  (-> (res/get
       (add-query-params (str "http://api.wordnik.com/api/word.json/" word "/definitions") ["count" "1"])
       {"api_key" wordnik-key})
      :body-seq first json/decode-from-str first extract-stuff))

(defplugin 
  (:dict 
   "Takes a word and look's up it's definition via the Wordnik dictionary API." 
   ["dict"] 
   [{:keys [irc channel nick args]}]
   (ircb/send-message irc channel 
		      (str nick ": " 
			   (let [[text part] (lookup-def (first args))]
			     (if (seq text) (str part ": " text) "Word not found."))))))