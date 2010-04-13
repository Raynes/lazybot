(ns sexpbot.plugins.title
  (:use sexpbot.respond
	[clojure.contrib.duck-streams :only [slurp*]]))

(def titlere #"(?i)<title>([^<]+)</title>")

(defn collapse-whitespace [s]
  (->> s (.split #"\s+") (interpose " ") (apply str)))

(defn add-url-prefix [url]
  (if-not (.startsWith url "http://")
    (str "http://" url)
    url))

(defn slurp-or-default [url]
  (try
   (slurp* url)
   (catch java.lang.Exception e nil)))

(defmethod respond :title* [{:keys [bot channel args verbose?]}]
  (if (seq args)
    (doseq [link (take 3 args)]
      (let [url (add-url-prefix (first args))
	    page (slurp-or-default url)
	    match (re-find titlere page)]
	(if (and (seq page) (seq match))
	  (.sendMessage bot channel (collapse-whitespace (second match)))
	  (when verbose? (.sendMessage bot channel "Page has no title.")))))
    (when verbose? (.sendMessage bot channel "Which page?"))))

(defmethod respond :title2 [botmap] (respond (assoc botmap :command "title*")))
(defmethod respond :title [botmap] (respond (assoc botmap :command "title*" :verbose? true)))

(defplugin
  {"title"  :title
   "title2" :title2
   "title*" :title*})