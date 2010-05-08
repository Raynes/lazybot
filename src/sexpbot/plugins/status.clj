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
    (alter statusmsg-map assoc-in [(:server @irc) nick]
	   {:status :away 
	    :msg (let [msg (.trim
			    (->> args
				 (interpose " ")
				 (apply str)))]
		   (if (seq msg)
		     msg
		     "I'm away."))})))
   (:return
    "Return from being away."
    ["return"]
    [{:keys [irc nick channel] :as irc-map}]
    (dosync
     (alter statusmsg-map assoc (:server @irc) {nick {:status :active :msg ""}})))

   (:status
    "Get the status of a user."
    ["status"]
    [{:keys [irc nick channel args] :as irc-map}]
    (let [user (.trim (or (first args) ""))]
      (if (seq user)
	(let [status-m (@statusmsg-map (:server @irc))]
	  (if-let [status-map (status-m user)]
	    (if (= :away (get status-map :status))
	      (ircb/send-message irc channel (str user " is away: " (status-map :msg)))
	      (ircb/send-message irc channel (str user " is active. ")))
	    (ircb/send-message irc channel (str (first args) " doesn't exist, or hasn't set their status"))))
	(ircb/send-message irc channel "Who?")))))