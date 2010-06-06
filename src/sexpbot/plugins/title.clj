;; The result of a team effort between programble and Rayne.
(ns sexpbot.plugins.title
  (:use [sexpbot info respond utilities]
	[clojure.contrib [string :only [ltrim]] [io :only [reader]]]
	[clj-config.core :only [read-config]])
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

(def url-blacklist-words (:url-blacklist (read-config info-file)))

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
  (let [blacklist (:user-ignore-url-blacklist ((read-config info-file) server))]
    (some (comp not nil?) (map 
			   #(is-blacklisted? % (strip-tilde user)) 
			   blacklist))))

(defn title [{:keys [irc nick user channel]} links & {verbose? :verbose? :or {verbose? false}}]
  (if (or (and verbose? (seq links))
	  (and (not (check-blacklist (:server @irc) user))
	       (not ((:channel-catch-blacklist ((read-config info-file) (:server @irc))) channel))))
    (doseq [link (take 1 links)]
      (try
       (thunk-timeout #(let [url (add-url-prefix link)
			     page (slurp-or-default url)
			     match (second page)]
			 (if (and (seq page) (seq match) (not (url-check url)))
			   (ircb/send-message irc channel 
					      (str "\"" 
						   (ltrim 
						    (StringEscapeUtils/unescapeHtml 
						     (collapse-whitespace match))) 
						   "\""))
			   (when verbose? (ircb/send-message irc channel "Page has no title."))))
		      20)
       (catch TimeoutException _ 
	 (when verbose? 
	   (ircb/send-message irc channel "It's taking too long to find the title. I'm giving up.")))))
    (when verbose? (ircb/send-message irc channel "Which page?"))))

(defplugin
  (:add-hook :on-message
	     (fn [{:keys [irc nick channel message] :as irc-map}]
	       (let [info (read-config info-file)
		     get-links (fn [s] (->> s (re-seq #"(http://|www\.)[^ ]+") (apply concat) (take-nth 2)))]
		 (when (not (:user-blacklist (info (:server @irc)) nick))
		   (let [prepend (:prepend info)
			 links (get-links message)
			 title-links? (and (not= prepend (first message)) 
					   ((:catch-links? info) (:server @irc))
					   (seq links))]
		     (when title-links?
		       (title irc-map links)))))))

  (:title 
   "Gets the title of a web page. Takes a link. This is verbose, and prints error messages."
   ["title"] [irc-map] (title irc-map (:args irc-map) :verbose? true)))