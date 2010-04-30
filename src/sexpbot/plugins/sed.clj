(ns sexpbot.plugins.sed
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(def prepend (:prepend (read-config)))
(def message-map (ref {}))

(defn- format-msg [{:keys [irc nick channel]}]
  (ircb/send-message irc channel (str nick ": Format is sed [-<user name>] s/<regexp>/<replacement>/  try $help sed")))
    
(defn sed [string regexp replacement]
    (.replaceAll string (str "(?i)" "" regexp) replacement))

(defplugin
  (:add-hook :on-message
	     (fn [{:keys [irc nick message channel] :as irc-map}]
	       (when (and (not= nick (:name @irc))
			  (not= (take 4 message) (cons (:prepend (read-config)) "sed")))
		 (dosync
		  (alter message-map assoc-in [irc channel nick] message)
		  (alter message-map assoc-in [irc channel :channel-last] message )))))

  (:sed 
   "Simple find and replace. Usage: sed [-<user name>] s/<regexp>/<replacement>/
    If the specified user isn't found, it will default to the last thing said in the channel.
    Example Usage: sed -boredomist s/[aeiou]/#/"
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
	 ;; Options temporarily removed
     
     (cond
      (empty? last-in) (ircb/send-message irc channel "No one said anything yet!")
      (not-any? seq [regexp replacement]) (format-msg irc-map)
      :else (ircb/send-message irc channel (str (sed last-in regexp replacement)))))))
