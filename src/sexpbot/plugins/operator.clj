(ns sexpbot.plugins.operator
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]))

(defmethod respond :op [{:keys [bot sender channel args]}]
  (if-admin sender (ircb/set-mode bot channel "+o" (first args))))

(defmethod respond :deop [{:keys [bot sender channel args]}]
  (if-admin sender (ircb/set-mode bot channel  "-o" (first args))))

(defmethod respond :kick [{:keys [bot sender channel args]}]
  (if-admin sender (ircb/send-msg "KICK" bot channel (str (first args) ":" 
							  (apply str (interpose " " (rest args)))))))

(defmethod respond :settopic [{:keys [bot sender channel args]}]
  (if-admin sender (ircb/send-msg "TOPIC" bot channel (str ":" (apply str (interpose " " args))))))

(defmethod respond :ban [{:keys [bot sender channel args]}]
  (if-admin sender (ircb/set-mode bot channel "+b" (first args))))

(defmethod respond :unban [{:keys [bot sender channel args]}]
  (if-admin sender (ircb/set-mode bot channel "-b" (first args))))

(defmethod respond :voice [{:keys [bot channel sender args]}]
  (if-admin sender (ircb/set-mode bot channel "+v" (first args))))

(defmethod respond :devoice [{:keys [bot channel sender args]}]
  (if-admin sender (ircb/set-mode bot channel "-v" (first args))))

(defplugin
  {"op"       :op
   "deop"     :deop
   "kick"     :kick
   "settopic" :settopic
   "ban"      :ban
   "unban"    :unban
   "voice"    :voice
   "devoice"  :devoice})