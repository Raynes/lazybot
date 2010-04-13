(ns sexpbot.plugins.title
  (:use [sexpbot info respond]
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

(def url-blacklist-words ((read-config) :url-blacklist))

(defn url-check [url]
  (some #(.contains url %) url-blacklist-words))

(defn is-blacklisted? [[match-this not-this] s]
  (let [lower-s (.toLowerCase s)] 
    (re-find (re-pattern (format "(?=.*%s(?!%s))^(\\w+)" match-this not-this)) s)))

(defn check-blacklist [& args]
  (let [blacklist ((read-config) :user-ignore-url-blacklist)]
    (some (comp not nil?) (for [x blacklist y args] (is-blacklisted? x y)))))

(defmethod respond :title* [{:keys [bot sender channel login host args verbose?]}]
  (if (or (and verbose? (seq args)) 
	  (and (seq args) 
	       (not (check-blacklist sender login host))
	       (not (((read-config) :channel-catch-blacklist) channel))))
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