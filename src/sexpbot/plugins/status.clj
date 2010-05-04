(ns sexpbot.plugins.status
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]))

(def statusmsg-map (ref {}))

(defplugin	 
  (:setaway
   "Sets your away status."
   ["setaway"]
   [{:keys [irc nick channel args] :as irc-map}]
   (dosync
    (alter statusmsg-map assoc irc {nick {:status :away :msg (let [msg (.trim
									(->> args
									     (interpose " ")
									     (apply str)))]
							       (if (seq msg)
								 msg
								 "I'm away."))}})))  
   (:return
    "Return from being away."
    ["return"]
    [{:keys [irc nick channel] :as irc-map}]
    (dosync
     (alter statusmsg-map assoc irc {nick {:status :active :msg ""}})))

   (:status
    "Get the status of a user."
    ["status"]
    [{:keys [irc nick channel args] :as irc-map}]
    (let [user (.trim (or (first args) ""))]
      (if (seq user)
	(let [status-map (get (get @statusmsg-map irc) user)]
	  (if status-map
	    (if (= :away (get status-map :status))
	      (ircb/send-message irc channel (str nick " is away: " (status-map :msg)))
	      (ircb/send-message irc channel (str nick " is active. ")))
	    (ircb/send-message irc channel (str (first args) " doesn't exist, or hasn't set their status"))))
	(ircb/send-message irc channel "Who?")))))