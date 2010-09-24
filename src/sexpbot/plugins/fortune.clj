; Written by Programble <programble@gmail.com>
; Licensed under the EPL
(ns sexpbot.plugins.fortune
  (:use [sexpbot respond info]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin
  (:cmd
   "Tells you what your fortune is. :>"
   #{"fortune"} 
   (fn [{:keys [irc bot channel args]}]
     (let [db (fetch :fortune)]
       (if (zero? (count db))
         (send-message irc bot channel "I have no fortune cookies. Please feed me some!")
         (send-message irc bot channel (:fortune (rand-nth db)))))))

  (:cmd
   "Adds a fortune to the fortune database."
   #{"addfortune"} 
   (fn [{:keys [irc bot channel args]}]
     (if (seq args)
       (do
         (insert! :fortune {:fortune (->> args (interpose " ") (apply str))})
         (send-message irc bot channel "Fortune cookie eaten."))
       (send-message irc bot channel "An invisible fortune cookie?")))))