(ns sexpbot.plugins.title
  (:use sexpbot.respond
	[clojure.contrib.duck-streams :only [reader]]))

(def titlere #"(?i)<title>([^<]+)</title>")

(defn collapse-whitespace [s]
  (->> s (.split #"\s+") (interpose " ") (apply str)))

(defn add-url-prefix [url]
  (if-not (.startsWith url "http://")
    (str "http://" url)
    url))

(defn slurp-or-default [url]
  (try
   (with-open [readerurl (reader url)]
     (some #(re-find titlere %) (line-seq readerurl)))
   (catch java.lang.Exception e nil)))

(def url-blacklist-words #{"paste" "gist"})

(defn url-check [url]
  (some #(.contains url %) url-blacklist-words))

(defmethod respond :title* [{:keys [bot channel args verbose?]}]
  (if (seq args)
    (doseq [link (take 1 args)]
      (let [url (add-url-prefix link)
	    page (slurp-or-default url)
	    match (second page)]
	(if (and (seq page) (seq match) (not (url-check url)))
	  (.sendMessage bot channel (collapse-whitespace match))
	  (when verbose? (.sendMessage bot channel "Page has no title.")))))
    (when verbose? (.sendMessage bot channel "Which page?"))))

(defmethod respond :title2 [botmap] (respond (assoc botmap :command "title*")))
(defmethod respond :title [botmap] (respond (assoc botmap :command "title*" :verbose? true)))

(defplugin
  {"title"  :title
   "title2" :title2
   "title*" :title*})