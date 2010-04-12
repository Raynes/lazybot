(ns sexpbot.plugins.rss
  (:use sexpbot.respond)
  (:require [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf]))

(defn cull [zipper]
  (let [items (take 3 (zf/xml-> zipper :channel :item))
	items2 (take 3 (zf/xml-> zipper :item))
	items3 (take 3 (zf/xml-> zipper :entry))]
    (map (fn [item] 
	   [(first (zf/xml-> item :title zf/text)) 
	    (first (if-let [atom-link (seq (zf/xml-> item :link (zf/attr :href)))]
		     atom-link
		     (zf/xml-> item :link zf/text)))]) 
	 (cond (seq items)  items 
	       (seq items2) items2
	       (seq items3) items3))))

(defn pull-feed [url]
  (-> url xml/parse zip/xml-zip cull))

(defmethod respond :rss [{:keys [bot channel args]}]
  (try
   (doseq [[title link] (pull-feed (first args))]
     (.sendMessage bot channel (str title " -- " link)))
   (catch Exception _ (.sendMessage bot channel "Feed is unreadable."))))

(defplugin
  {"rss" :rss})