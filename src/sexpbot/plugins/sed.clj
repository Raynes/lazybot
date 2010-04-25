(ns sexpbot.plugins.sed
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(def prepend (:prepend (read-config)))

(defn sed [string expr]
  (let [results (rest (re-find #"s/([^/]+)/([^/]*)/" expr))]
    (.replaceAll string (first results) (last results))))

(defmethod respond :sed [{:keys [irc channel args]}]
  (let [conj-args (apply str (interpose " " (rest args)))
	last-in (when-let [last-in* (:last-in @irc)] 
		  (last-in* (first args)))]
    (when (seq last-in)
      (ircb/send-message irc channel "No one said anything yet!"))
    (when (re-matches #"s/[^/]+/[^/]*/" conj-args)
      (ircb/send-message irc channel (sed last-in conj-args))
      (when (= (first last-in) prepend)
	(respond (rest (sed last-in conj-args)))))
    (when (not (re-matches #"s/[^/]+/[^/]*/" conj-args))
      (ircb/send-message irc channel "Format: sed channel s/regex/replacement/"))))

(defplugin {"sed" :sed})
