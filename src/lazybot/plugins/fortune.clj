; Written by Programble <programble@gmail.com>
; Licensed under the EPL
(ns lazybot.plugins.fortune
  (:use [lazybot registry info]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin
  (:cmd
   "Tells you what your fortune is. :>"
   #{"fortune"} 
   (fn [com-m]
     (let [db (fetch :fortune)]
       (if (zero? (count db))
         (send-message com-m "I have no fortune cookies. Please feed me some!")
         (send-message com-m (:fortune (rand-nth db)))))))

  (:cmd
   "Adds a fortune to the fortune database."
   #{"addfortune"} 
   (fn [{:keys [args] :as com-m}]
     (if (seq args)
       (do
         (insert! :fortune {:fortune (->> args (interpose " ") (apply str))})
         (send-message com-m "Fortune cookie eaten."))
       (send-message com-m "An invisible fortune cookie?")))))