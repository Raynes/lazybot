(ns sexpbot.plugins.dynamic
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]))

(def char-map (apply hash-map (interleave (range 1 28) "abcdefghijklmnopqrstuvwxyz ")))

(defn s-to-n [point]
  (condp = point
    \< -1
    \^ 5
    \> 1
    \& 27
    0))

(defn symset-to-chars [point-set]
  (apply + (map s-to-n point-set)))

(defn dynamic-to-str [dystr]
  (->> dystr
       (#(.replaceAll % "&" ".&."))
       (#(.split % "\\.")) 
       (filter seq)
       (map symset-to-chars)
       (map char-map)
       (apply str)))

(defmethod respond :dytostr [{:keys [irc channel args]}]
  (ircb/send-message irc channel (dynamic-to-str (first args))))

(defplugin
  {"dytostr" :dytostr})