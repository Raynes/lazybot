(ns sexpbot.plugins.google
  (:use [sexpbot respond]
	[clojure.contrib.duck-streams :only [slurp*]])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http]))

(defn google [term]
  (-> (http/get (java.net.URI. "http://ajax.googleapis.com/ajax/services/search/web")
		:query {:v "1.0", :q term} :as :string) :content json/decode-from-str))

(defn cull [result-set]
  [(:estimatedResultCount (:cursor (:responseData result-set)))
   (first (:results (:responseData result-set)))])

(defn handle-search [{:keys [bot channel args]}]
  (let [[res-count res-map] (-> (apply str (interpose " " args)) google cull)
	title (:titleNoFormatting res-map)
	url (:url res-map)]
    (.sendMessage bot channel (str "First out of " res-count " results is:"))
    (.sendMessage bot channel title)
    (.sendMessage bot channel url)))

(defmethod respond :google [args]
  (handle-search args))

(defmethod respond :wiki [args]
  (handle-search (assoc args :args (conj (:args args) "site:en.wikipedia.org"))))

(defmethod respond :ed [args]
  (handle-search (assoc args :args (conj (:args args) "site:encyclopediadramtica.com"))))

(defplugin 
  {"google" :google
   "wiki"   :wiki
   "ed"     :ed})