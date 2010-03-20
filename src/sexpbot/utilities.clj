(ns sexpbot.utilities)

(defn stringify [coll]
  (apply str (interpose " " coll)))
