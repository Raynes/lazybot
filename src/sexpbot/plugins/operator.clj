(ns sexpbot.plugins.operator
  (:use [sexpbot respond]))

(defmethod respond :op [{:keys [bot sender channel args]}]
  (if-admin sender (.op bot channel (first args))))

(defmethod respond :deop [{:keys [bot sender channel args]}]
  (if-admin sender (.deOp bot channel (first args))))

(defmethod respond :kick [{:keys [bot sender channel args]}]
  (if-admin sender (.kick bot channel (first args))))

(defmethod respond :settopic [{:keys [bot sender channel args]}]
  (if-admin sender (.setTopic bot channel (apply str (interpose " " args)))))

(defmethod respond :ban [{:keys [bot sender channel args]}]
  (if-admin sender (.ban bot channel (first args))))

(defmethod respond :unban [{:keys [bot sender channel args]}]
  (if-admin sender (.unBan bot channel (first args))))

(defmethod respond :voice [{:keys [bot channel sender args]}]
  (if-admin sender (.voice bot channel (first args))))

(defmethod respond :devoice [{:keys [bot channel sender args]}]
  (if-admin sender (.deVoice bot channel (first args))))

(defplugin
  {"op"       :op
   "deop"     :deop
   "kick"     :kick
   "settopic" :settopic
   "ban"      :ban
   "unban"    :unban
   "voice"    :voice
   "devoice"  :devoice})