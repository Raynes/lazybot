(ns sexpbot.plugins.rss
  (:use sexpbot.respond
	[sexpbot.plugins.shorturl :only [shorten-url]])
  (:require [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.contrib.zip-filter.xml :as zf]
	    ))

(defn cull [zipper]
  (let [items (take 3 (zf/xml-> zipper :channel :item))
	items2 (take 3 (zf/xml-> zipper :item))
	items3 (take 3 (zf/xml-> zipper :entry))]
    (map (fn [item] 
	   [(first (zf/xml-> item :title zf/text)) 
	    (shorten-url (first (if-let [atom-link (seq (zf/xml-> item :link (zf/attr :href)))]
				  atom-link
				  (zf/xml-> item :link zf/text))) "isgd")]) 
	 (cond (seq items)  items 
	       (seq items2) items2
	       (seq items3) items3))))

(defn pull-feed [url]
  (-> url xml/parse zip/xml-zip cull))

(defplugin
  (:rss 
   "Get's the first three results from an RSS or Atom feed."
   ["rss" "atom"] 
   [{:keys [irc bot channel args]}]
   (try
    (doseq [[title link] (pull-feed (first args))]
      (send-message irc bot channel (str title " -- " link)))
    (catch Exception _ (send-message irc channel "Feed is unreadable.")))))