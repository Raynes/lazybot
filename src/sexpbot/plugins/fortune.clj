; Written by Programble <programble@gmail.com>
; Licensed under the EPL
(ns sexpbot.plugins.fortune
  (:use [sexpbot respond info gist])
  (:require [irclj.irclj :as ircb]))

; Database file
(def fortunefile (str sexpdir "/fortunes.clj"))

(defmethod respond :fortune [{:keys [bot channel args]}]
  (with-info fortunefile
    (let [db (:fortunes (read-config))]
      ; Check if database is empty
      (if (zero? (count db))
        (ircb/send-message bot channel "I have no fortune cookies. Please feed me some!")
        (ircb/send-message bot channel (nth db (rand-int (count db))))))))

(defmethod respond :addfortune [{:keys [bot channel args]}]
  (if (seq args)
    (with-info fortunefile
      (let [new-fortune (->> args (interpose " ") (apply str))
            db (read-config)]
        (write-config (assoc db :fortunes (conj (:fortunes db) new-fortune)))
        (ircb/send-message bot channel "Fortune cookie eaten.")))
    (ircb/send-message bot channel "An invisible fortune cookie?")))

(defmethod respond :dumpfortunes [{:keys [bot channel args]}]
  (with-info fortunefile
    (let [db (:fortunes (read-config))
          dump (->> db (interpose "\n") (apply str))]
      (ircb/send-message bot channel (post-gist "Fortunes Dump" dump)))))

(defplugin
  {"fortune"      :fortune
   "addfortune"   :addfortune
   "dumpfortunes" :dumpfortunes})
