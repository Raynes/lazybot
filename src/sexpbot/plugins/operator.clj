(ns sexpbot.plugins.operator
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]))

(defmethod respond :op [{:keys [irc nick channel args]}]
  (if-admin nick (ircb/set-mode irc channel "+o" (first args))))

(defmethod respond :deop [{:keys [irc nick channel args]}]
  (if-admin nick (ircb/set-mode irc channel  "-o" (first args))))

(defmethod respond :kick [{:keys [irc nick channel args]}]
  (if-admin nick (ircb/kick irc channel (first args) (apply str (rest args)))))

(defmethod respond :settopic [{:keys [irc nick channel args]}]
  (if-admin nick (ircb/set-topic irc channel (apply str (interpose " " args)))))

(defmethod respond :ban [{:keys [irc nick channel args]}]
  (if-admin nick (ircb/set-mode irc channel "+b" (first args))))

(defmethod respond :unban [{:keys [irc nick channel args]}]
  (if-admin nick (ircb/set-mode irc channel "-b" (first args))))

(defmethod respond :voice [{:keys [irc channel nick args]}]
  (if-admin nick (ircb/set-mode irc channel "+v" (first args))))

(defmethod respond :devoice [{:keys [irc channel nick args]}]
  (if-admin nick (ircb/set-mode irc channel "-v" (first args))))

(defplugin
  {"op"       :op
   "deop"     :deop
   "kick"     :kick
   "settopic" :settopic
   "ban"      :ban
   "unban"    :unban
   "voice"    :voice
   "devoice"  :devoice})