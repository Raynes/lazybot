(ns sexpbot.plugins.google
  (:use [sexpbot respond]
	[clojure-http.client :only [add-query-params]])
  (:require [org.danlarkin.json :as json]
	    [clojure-http.resourcefully :as res]
	    )
  (:import org.apache.commons.lang.StringEscapeUtils))

(defn google [term]
  (-> (res/get (add-query-params "http://ajax.googleapis.com/ajax/services/search/web"
				 {"v" "1.0" "q" term}))
      :body-seq first json/decode-from-str))

(defn cull [result-set]
  [(:estimatedResultCount (:cursor (:responseData result-set)))
   (first (:results (:responseData result-set)))])

(defn handle-search [{:keys [irc bot channel args]}]
  (if-not (seq (.trim (apply str (interpose " " args))))
    (send-message irc bot channel (str "No search term!"))
    (let [[res-count res-map] (-> (apply str (interpose " " args)) google cull)
	  title (:titleNoFormatting res-map)
	  url (:url res-map)]
      (send-message irc bot channel (StringEscapeUtils/unescapeHtml 
				      (str "First out of " res-count " results is: " title)))
      (send-message irc bot channel url))))

(defplugin
  (:cmd
   "Searches google for whatever you ask it to, and displays the first result and the estimated
   number of results found."
   #{"google"} 
   #'handle-search)

   (:cmd
    "Searches wikipedia via google."
    #{"wiki"}
    (fn [args]
      (handle-search (assoc args :args (conj (:args args) "site:en.wikipedia.org")))))

   (:cmd
    "Searches encyclopediadramtica via google."
    #{"ed"} 
    (fn [args]
      (handle-search (assoc args :args (conj (:args args) "site:encyclopediadramatica.com"))))))