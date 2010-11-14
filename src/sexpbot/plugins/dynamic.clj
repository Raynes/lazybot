(ns sexpbot.plugins.dynamic
  (:use [sexpbot registry]))

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

(defplugin 
  (:cmd
   "Converts a dynamic string to a English string."
   #{"dy2str" "dytostr"}
   (fn [{:keys [irc bot channel args]}]
     (if-not (seq (.trim (apply str (interpose " "  args))))
       (send-message irc bot channel "No dynamic string given!")
       (send-message irc bot channel (dynamic-to-str (first args)))))))