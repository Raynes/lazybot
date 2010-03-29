(ns sexpbot.plugins.dynamic)

(def char-map 
     (apply hash-map 
	    (interleave (range 1 27) "abcdefghijklmnopqrstuvwxyz")))

(defn s-to-n [point]
  (condp = point
    \< -1
    \^ 5
    \> 1
    0))

(defn symset-to-chars [point-set]
  (apply + (map s-to-n point-set)))

(defn dynamic-to-str [dystr]
  (->> dystr 
       (remove #(= \& %))
       (apply str)
       (#(.split % "\\.")) 
       (filter seq)
       (map symset-to-chars)
       (map char-map)
       (interpose " ")
       (apply str)))