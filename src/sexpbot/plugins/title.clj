;; The result of a team effort between programble and Rayne.
(ns sexpbot.plugins.title
  (:use [sexpbot [info :only [read-config]] respond utilities]
	[clojure.contrib.io :only [reader]])
  (:require [irclj.irclj :as ircb])
  (:import java.util.concurrent.TimeoutException
	   org.apache.commons.lang.StringEscapeUtils))

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
     (loop [acc [] lines (line-seq readerurl)]
       (cond
	(not (seq lines)) nil
	(some #(re-find #"</title>|</TITLE>" %) acc) (->> acc (apply str) 
							  (#(.replace % "\n" " ")) 
							  (re-find titlere))
	:else (recur (conj acc (first lines)) (rest lines)))))
   (catch java.lang.Exception e nil)))

(def url-blacklist-words ((read-config) :url-blacklist))

(defn url-check [url]
  (some #(.contains url %) url-blacklist-words))

(defn is-blacklisted? [[match-this not-this] s]
  (let [lower-s (.toLowerCase s)
	regex (if (seq not-this)
		(re-pattern (format "(?=.*%s(?!%s))^(\\w+)" match-this not-this))
		(re-pattern match-this))]
    (re-find regex lower-s)))

(defn strip-tilde [s] (apply str (remove #(= \~ %) s)))

(defn check-blacklist [server user]
  (let [blacklist (((read-config) :user-ignore-url-blacklist) server)]
    (some (comp not nil?) (map 
			   #(is-blacklisted? % (strip-tilde user)) 
			   blacklist))))

(defplugin
  (:add-hook :on-message
	     (fn [{:keys [irc nick channel message] :as irc-map}]
	       (let [info (read-config)
		     get-links (fn [s] (->> s (re-seq #"(http://|www\.)[^ ]+") (apply concat) (take-nth 2)))]
		 (when (not (((info :user-blacklist) (:server @irc)) nick))
		   (let [prepend (:prepend info)
			 links (get-links message)
			 title-links? (and (not= prepend (first message)) 
					   ((:catch-links? info) (:server @irc))
					   (seq links))]
		     (when title-links? 
		       (try-handle 
			(assoc irc-map :message (str prepend "title2 " (apply str (interpose " " links)))))))))))

  (:title*
   "A utility method to get the title of a webpage. Non-verbose, so it doesn't
   print error messages. Use $title instead."
   ["title*"]
   [{:keys [irc nick user channel args verbose?]}]
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
			    (ircb/send-message irc channel 
					       (str "\"" 
						    (StringEscapeUtils/unescapeHtml (collapse-whitespace match)) 
						    "\""))
			    (when verbose? (ircb/send-message irc channel "Page has no title."))))
		       20)
	(catch TimeoutException _ 
	  (when verbose? 
	    (ircb/send-message irc channel "It's taking too long to find the title. I'm giving up.")))))
     (when verbose? (ircb/send-message irc channel "Which page?"))))
  
  (:title2
   "Get's the title of a webpage. Just takes a link." 
   ["title2"] [ircmap] (respond (assoc ircmap :command "title*")))

  (:title 
   "Get's the title of a web page. Takes a link. This is verbose, and prints error messages."
   ["title"] [ircmap] (respond (assoc ircmap :command "title*" :verbose? true))))