(ns sexpbot.plugins.sed
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]
	    [sexpbot.core :as core]))

(defn sed [string expr]
  (def results (rest (re-find #"s/([^/]+)/([^/]*)/" expr)))
;;  (let [ ;;FIX THIS UGLINESS
  (.replaceAll string (first results) (last results)))

(defmethod respond :sed [{:keys [irc channel args]}]
  (let [conj-args (apply str (interpose " " args))
	last-in core/last-in]
    
   (when (= core/last-in "")
     (ircb/send-message irc channel "No one said anything yet!"))
   (when (re-matches #"s/[^/]+/[^/]*/"  conj-args)
     (ircb/send-message irc channel (sed last-in conj-args))
     (when (= (first last-in) core/prepend) ;; FIXME --currently throws NPE (need to pass irc-map type hashmap)
       (respond (rest (sed last-in conj-args)))))
   (when (not (re-matches #"s/[^/]+/[^/]*/"  conj-args))
     (ircb/send-message irc channel "Format: sed s/regex/replacement/"))))
  
  
  
(defplugin {"sed" :sed})
