; Written by Programble <programble@gmail.com>
; Licensed under the GNU GPLv3
(ns sexpbot.plugins.fortune
	(:use [sexpbot respond info]))

; Database file
(def fortunefile (str sexpdir "/fortunes.clj"))

(defmethod respond :fortune [{:keys [bot channel args]}]
  (with-info fortunefile
    (let [db (:fortunes (read-config))]
      ; Check if database is empty
      (if (zero? (count db))
        (.sendMessage bot channel "I have no fortune cookies. Please feed me some!")
        (.sendMessage bot channel (nth db (rand-int (count db))))))))

(defplugin
  {"fortune"  :fortune})
