(ns lazybot.plugins.rss
  (:require [lazybot.registry :as registry]
            [lazybot.utilities :refer [shorten-url]]
            [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.data.zip.xml :as zf]))

(defn cull [zipper]
  (let [items (take 3 (zf/xml-> zipper :channel :item))
	items2 (take 3 (zf/xml-> zipper :item))
	items3 (take 3 (zf/xml-> zipper :entry))]
    (map (fn [item] 
	   [(first (zf/xml-> item :title zf/text)) 
	    (shorten-url (first (if-let [atom-link (seq (zf/xml-> item :link (zf/attr :href)))]
				  atom-link
				  (zf/xml-> item :link zf/text))))])
	 (cond (seq items)  items 
	       (seq items2) items2
	       (seq items3) items3))))

(defn pull-feed [url]
  (-> url xml/parse zip/xml-zip cull))

(registry/defplugin
  (:cmd
   "Get's the first three results from an RSS or Atom feed."
   #{"rss" "atom"} 
   (fn rss-plugin [{:keys [bot channel args] :as com-m}]
     (try
       (doseq [[title link] (pull-feed (first args))]
         (registry/send-message com-m (str title " -- " link)))
       (catch Exception e
         (println e)
         (.printStackTrace e)
         (registry/send-message com-m "Feed is unreadable."))))))
