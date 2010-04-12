; Written by Programble <programble@gmail.com>
; Licensed under the EPL
(ns sexpbot.plugins.fortune
	(:use [sexpbot respond info gist]))

; Database file
(def fortunefile (str sexpdir "/fortunes.clj"))

(defmethod respond :fortune [{:keys [bot channel args]}]
  (with-info fortunefile
    (let [db (:fortunes (read-config))]
      ; Check if database is empty
      (if (zero? (count db))
        (.sendMessage bot channel "I have no fortune cookies. Please feed me some!")
        (.sendMessage bot channel (nth db (rand-int (count db))))))))

(defmethod respond :addfortune [{:keys [bot channel args]}]
  (if (seq args)
    (with-info fortunefile
      (let [new-fortune (->> args (interpose " ") (apply str))
            db (read-config)]
        (write-config (assoc db :fortunes (conj (:fortunes db) new-fortune)))
        (.sendMessage bot channel "Fortune cookie eaten.")))
    (.sendMessage bot channel "An invisible fortune cookie?")))

(defmethod respond :dumpfortunes [{:keys [bot channel args]}]
  (with-info fortunefile
    (let [db (:fortunes (read-config))
          dump (->> db (interpose "\n") (apply str))]
      (.sendMessage bot channel (post-gist "Fortunes Dump" dump)))))

(defplugin
  {"fortune"      :fortune
   "addfortune"   :addfortune
   "dumpfortunes" :dumpfortunes})
