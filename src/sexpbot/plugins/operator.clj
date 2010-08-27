(ns sexpbot.plugins.operator
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb])
  )

(defplugin
  (:op 
   "Sets the person you specify as operator. ADMIN ONLY."
   ["op"] 
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/set-mode irc channel "+o" (first args))))
  
  (:deop 
   "Deops the person you specify. ADMIN ONLY."
   ["deop"] 
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/set-mode irc channel  "-o" (first args))))

  (:kick 
   "Kicks the person you specify. ADMIN ONLY."
   ["kick"] 
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/kick irc channel (first args) :reason (apply str (rest args)))))

  (:settopic 
   "Set's the channel's topic. ADMIN ONLY."
   ["settopic"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/set-topic irc channel (apply str (interpose " " args)))))

  (:ban 
   "Ban's whatever you specify. ADMIN ONLY."
   ["ban"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/set-mode irc channel "+b" (first args))))

  (:unban 
   "Unban's whatever you specify. ADMIN ONLY."
   ["unban"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/set-mode irc channel "-b" (first args))))

  (:voice 
   "Voices the person you specify. ADMIN 0NLY."
   ["voice"] 
   [{:keys [irc bot channel nick args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/set-mode irc channel "+v" (first args))))

  (:devoice 
   "Devoices the person you specify. ADMIN ONLY."
   ["devoice"]
   [{:keys [irc bot channel nick args] :as irc-map}]
   (if-admin nick irc-map bot (ircb/set-mode irc channel "-v" (first args)))))
