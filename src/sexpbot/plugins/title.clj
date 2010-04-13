(ns sexpbot.plugins.title
  (:use sexpbot.respond
	[clojure.contrib.duck-streams :only [slurp*]]))

;;;; programble ;;;;
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
  (catch java.lang.Exception e
    "<html><head><title>Cannot load page</title></head></html>")))

(defmethod respond :title [{:keys [bot channel args]}]
  (if (seq args)
    (doseq [link (take 3 args)]
      (let [url (add-url-prefix link)
	    page (slurp-or-default url)
	    match (re-find titlere page)]
	(if (seq match)
	  (.sendMessage bot channel (collapse-whitespace (second match)))
	  (.sendMessage bot channel "Page has no title."))))
    (.sendMessage bot channel "Which page?")))
;;;;;;;;;;;;;;;;;;;

(defplugin
  {"title" :title})