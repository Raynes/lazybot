;; The result of a team effort between programble and Rayne.
(ns sexpbot.plugins.title
  (:use [sexpbot info respond utilities]
	[clojure.contrib.io :only [reader]])
  (:require [irclj.irclj :as ircb])
  (:import java.util.concurrent.TimeoutException))

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
  (println s)
  (let [lower-s (.toLowerCase s)
	regex (if (seq not-this)
		(re-pattern (format "(?=.*%s(?!%s))^(\\w+)" match-this not-this))
		(re-pattern match-this))]
    (re-find regex lower-s)))

(defn strip-tilde [s] (apply str (remove #(= \~ %) s)))

(defn check-blacklist [server user]
  (println user)
  (let [blacklist (((read-config) :user-ignore-url-blacklist) server)]
    (some (comp not nil?) (map 
			   #(is-blacklisted? % (strip-tilde user)) 
			   blacklist))))

(defmethod respond :title* [{:keys [irc nick user channel args verbose?]}]
  (if (or (and verbose? (seq args)) 
	  (and (seq args) 
	       (not (check-blacklist (:server @irc) user))
	       (not ((((read-config) :channel-catch-blacklist) (:server @irc)) channel))))
    (doseq [link (take 1 args)]
      (try
       (thunk-timeout #(let [url (add-url-prefix link)
			     page (slurp-or-default url)
			     match (second page)]
			 (if (and (seq page) (seq match) (not (url-check url)))
			   (ircb/send-message irc channel (str "\"" (collapse-whitespace match) "\""))
			   (when verbose? (ircb/send-message irc channel "Page has no title."))))
		      20)
       (catch TimeoutException _ 
	 (when verbose? 
	   (ircb/send-message irc channel "It's taking too long to find the title. I'm giving up.")))))
    (when verbose? (ircb/send-message irc channel "Which page?"))))

(defmethod respond :title2 [ircmap] (respond (assoc ircmap :command "title*")))
(defmethod respond :title [ircmap] (respond (assoc ircmap :command "title*" :verbose? true)))

(defplugin
  {"title"  :title
   "title2" :title2
   "title*" :title*})