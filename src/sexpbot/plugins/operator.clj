(ns sexpbot.plugins.operator
  (:use [sexpbot registry])
  (:require [irclj.irclj :as ircb]))

(defplugin
  (:cmd
   "Sets the person you specify as operator. ADMIN ONLY."
   #{"op"} 
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-mode irc channel "+o" (first args)))))
  
  (:cmd
   "Deops the person you specify. ADMIN ONLY."
   #{"deop"} 
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-mode irc channel  "-o" (first args)))))

  (:cmd
   "Kicks the person you specify. ADMIN ONLY."
   #{"kick"} 
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/kick irc channel (first args) :reason (apply str (rest args))))))

  (:cmd 
   "Set's the channel's topic. ADMIN ONLY."
   #{"settopic"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-topic irc channel (apply str (interpose " " args))))))

  (:cmd 
   "Ban's whatever you specify. ADMIN ONLY."
   #{"ban"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-mode irc channel "+b" (first args)))))

  (:cmd 
   "Unban's whatever you specify. ADMIN ONLY."
   #{"unban"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-mode irc channel "-b" (first args)))))

  (:cmd 
   "Voices the person you specify. ADMIN 0NLY."
   #{"voice"} 
   (fn [{:keys [irc bot channel nick args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-mode irc channel "+v" (first args)))))

  (:cmd 
   "Devoices the person you specify. ADMIN ONLY."
   #{"devoice"}
   (fn [{:keys [irc bot channel nick args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-mode irc channel "-v" (first args))))))
