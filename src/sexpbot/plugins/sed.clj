(ns sexpbot.plugins.sed
  (:use [sexpbot respond info]
	clj-config.core)
  )

(def prepends (:prepends (read-config info-file)))
(def message-map (atom {}))

(defn- format-msg [irc bot nick channel]
  (send-message irc bot channel (str nick ": Format is sed [-<user name>] s/<regexp>/<replacement>/ Try $help sed")))

(defn- conj-args [args]
  (->> args
       (interpose " ")
       (apply str)))

(defn sed* [string regexp replacement]
  (try
    (.replaceAll string (str "(?i)" regexp) replacement)))

(defn sed [irc bot channel nick args verbose?]
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
     (empty? last-in) (send-message irc bot channel "No one said anything yet!")
     (not-any? seq [regexp replacement]) (format-msg irc bot nick channel)
     :else (try
	     (let [orig-msg last-in
		   new-msg (sed* last-in regexp replacement)]
	       (when-not (= orig-msg new-msg) (send-message irc bot channel new-msg)))
	     (catch Exception _ (when verbose? (format-msg irc bot nick channel)))))))


(defplugin
  (:add-hook :on-message
	     (fn [{:keys [irc bot nick message channel] :as irc-map}]
	       (when (not-empty (re-find #"^s/([^/]+)/([^/]*)/" message))
		 (sed irc bot channel nick [(str "-" nick) message] false))
	       
	       (when (and (not= nick (:name @irc))
			  (not= (take 4 message) (str (first prepends) "sed")))
		 (swap! message-map assoc-in [irc channel nick] message)
		 (swap! message-map assoc-in [irc channel :channel-last] message))))
  
  (:sed
   "Simple find and replace. Usage: sed [-<user name>] s/<regexp>/<replacement>/
If the specified user isn't found, it will default to the last thing said in the channel.
Example Usage: sed -boredomist s/[aeiou]/#/
Shorthand : s/[aeiou]/#/"
   ["sed"]
   [{:keys [irc bot channel args nick] :as irc-map}] (sed irc bot channel nick args true)))

