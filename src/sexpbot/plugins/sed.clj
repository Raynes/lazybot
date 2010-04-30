(ns sexpbot.plugins.sed
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(def prepend (:prepend (read-config)))
(def message-map (ref {}))

; Feel free to write some new ones.
; -- 
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;     OPTIONS       ;;;
;; case insensitive -i ;;
;; don't execute -x    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;;<Raynes> boredomist: Make it so that specifying a channel and options are
;;	 unnecessary. Make it default to case insensitive and the current
;;	 channel.
;;<Raynes> boredomist: And make sure you pull my latest repo
(defn parse-opts [opt-string]
  {:case-inse (.contains opt-string "i")
   :no-exec (.contains opt-string "x")})
    
(defn sed [string expr]
  (let [[regexp replacement] (rest (re-find #"s/([^/]+)/([^/]*)/" expr))]
    (.replaceAll string (str "(?i)" "" regexp) replacement)))

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
	 margs (or (rest args) "")
	 conj-args  (->> margs
			 (interpose " ")
			 (apply str))
	 user-to (or
		   (second (re-find #"-([\w]+)" farg))		   
		  nil)
	 last-in (or
		  (((@message-map irc) channel) user-to)
		  (try
		   (((@message-map irc) channel) :channel-last)
		   (catch
		       NullPointerException e nil)))]
	 ;; Options temporarily removed
     
     (ircb/send-message irc channel (str "LASTIN: " last-in " TO: " user-to)))))






(comment     (cond (empty? last-in) (ircb/send-message irc channel "No one said anything there yet!")
	   (re-matches #"([a-zA-Z]*)[\s]+s/[^/]+/[^/]*/" conj-args) 
	   (do (ircb/send-message irc channel (sed last-in conj-args opts))
	       (when (and (= (first last-in) prepend)
			  (not (:no-exec opts)))
		 (handle-message (assoc irc-map :message (sed last-in conj-args opts)))))
	   :else (ircb/send-message irc channel "Format: sed channel [options] s/regex/replacement/ Options are: Case Insensitive: i Don't Execute: x")))
