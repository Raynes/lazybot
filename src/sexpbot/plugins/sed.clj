(ns sexpbot.plugins.sed
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(def prepend (:prepend (read-config)))
(def message-map (ref {}))

(defn- format-msg [{:keys [irc nick channel]}]
  (ircb/send-message irc channel (str nick ": Format is sed [-<user name>] s/<regexp>/<replacement>/
Try $help sed")))
    
(defn sed [string regexp replacement]
  (try
   (.replaceAll string (str "(?i)" regexp) replacement)
   (catch java.util.regex.PatternSyntaxException e (str "Incorrectly formatted regular expression: " regexp))))

(defplugin
  (:add-hook :on-message
	     (fn [{:keys [irc nick message channel] :as irc-map}]
	       (when (not-empty (re-find #"s/([^/]+)/([^/]*)/" message))
		 (try-handle (assoc irc-map :message (str prepend "sed " message))))
				
	       (when (and (not= nick (:name @irc))
			  (not= (take 4 message) (cons prepend "sed")))
		 (dosync
		  (alter message-map assoc-in [irc channel nick] message)
		  (alter message-map assoc-in [irc channel :channel-last] message )))))

  (:sed 
   "Simple find and replace. Usage: sed [-<user name>] s/<regexp>/<replacement>/
    If the specified user isn't found, it will default to the last thing said in the channel.
    Example Usage: sed -boredomist s/[aeiou]/#/
    Shorthand    : s/[aeiou]/#/"
   ["sed"]
   [{:keys [irc channel args] :as irc-map}]
   (let [farg (or (first args) "")
	 margs (if(not-empty (rest args))
		 (.trim (->> (rest args)
		      (interpose " ")
		      (apply str)))
		 farg)

	 user-to (or (second (re-find #"-([\w]+)" farg)) nil)
	 last-in (or
		  (try
		   (((@message-map irc) channel) user-to)
		   (catch NullPointerException e nil))
		  (try
		   (((@message-map irc) channel) :channel-last)
		   (catch
		       NullPointerException e nil)))
	 [regexp replacement] (or
			       (not-empty (rest (re-find #"s/([^/]+)/([^/]*)/" margs)))
			       nil)]     
     (cond
      (empty? last-in) (ircb/send-message irc channel "No one said anything yet!")
      (not-any? seq [regexp replacement]) (format-msg irc-map)
      :else (do
		(let [result (sed last-in regexp replacement)]
		  (ircb/send-message irc channel result)
		  (when (= (first result) prepend)
		    (try-handle (assoc irc-map :message result)))))))))
