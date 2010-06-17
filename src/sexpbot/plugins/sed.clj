(ns sexpbot.plugins.sed
  (:use [sexpbot respond info]
	clj-config.core)
  (:require [irclj.irclj :as ircb]))

(def prepends (:prepends (read-config info-file)))
(def message-map (atom {}))

(defn- format-msg [irc nick channel]
  (ircb/send-message irc channel (str nick ": Format is sed [-<user name>] s/<regexp>/<replacement>/ Try $help sed")))
(defn- conj-args [args]
  (->> args
       (interpose " ")
       (apply str)))

(defn sed* [string regexp replacement]
  (try
   (.replaceAll string (str "(?i)" regexp) replacement)
   (catch java.util.regex.PatternSyntaxException e (str "Incorrectly formatted regular expression: " regexp))))

(defn sed [irc channel nick args]
  (let [user-to (or (second (re-find #"^[\s]*-([\w]+)" (.trim (conj-args args)))) "")
	margs (or (second (re-find #"[\s]*(s/[^/]+/[^/]*/)$" (.trim (conj-args args)))) "")
	
	last-in (or
		 (try
		   (((@message-map irc) channel) user-to)
		   (catch NullPointerException e nil))
		 (try
		   (((@message-map irc) channel) :channel-last)
		   (catch
		       NullPointerException e nil)))
	[regexp replacement] (or
			      (not-empty (rest (re-find #"^s/([^/]+)/([^/]*)/" margs)))
			      nil)]
    (cond
     (empty? last-in) (ircb/send-message irc channel "No one said anything yet!")
     (not-any? seq [regexp replacement]) (format-msg irc nick channel)
     :else (let [orig-msg last-in
		 new-msg (sed* last-in regexp replacement)]
	     (when-not (= orig-msg new-msg) (ircb/send-message irc channel new-msg))))))


(defplugin
  (:add-hook :on-message
	    (fn [{:keys [irc nick message channel] :as irc-map}]
	      (when (not-empty (re-find #"^s/([^/]+)/([^/]*)/" message))
		(sed irc channel nick [(str "-" nick) message]))
	      
	      (when (and (not= nick (:name @irc))
			 (not= (take 4 message) (str (first prepends) "sed")))
                (swap! message-map assoc-in [irc channel nick] message)
                (swap! message-map assoc-in [irc channel :channel-last] message))))
  
  (:sed 
   "Simple find and replace. Usage: sed [-<user name>] s/<regexp>/<replacement>/
    If the specified user isn't found, it will default to the last thing said in the channel.
    Example Usage: sed -boredomist s/[aeiou]/#/
    Shorthand    : s/[aeiou]/#/"
   ["sed"]
   [{:keys [irc channel args nick] :as irc-map}] (sed irc channel nick args)))
