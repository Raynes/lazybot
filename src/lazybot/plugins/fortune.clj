; Written by Programble <programble@gmail.com>
; Licensed under the EPL
(ns lazybot.plugins.fortune
  (:require [lazybot.registry :as registry]
            [somnium.congomongo :refer [fetch fetch-one insert! destroy!]]))

(registry/defplugin
  (:cmd
   "Tells you what your fortune is. :>"
   #{"fortune"} 
   (fn [com-m]
     (let [db (fetch :fortune)]
       (if (zero? (count db))
         (registry/send-message com-m "I have no fortune cookies. Please feed me some!")
         (registry/send-message com-m (:fortune (rand-nth db)))))))

  (:cmd
   "Adds a fortune to the fortune database."
   #{"addfortune"} 
   (fn [{:keys [args] :as com-m}]
     (if (seq args)
       (do
         (insert! :fortune {:fortune (->> args (interpose " ") (apply str))})
         (registry/send-message com-m "Fortune cookie eaten."))
       (registry/send-message com-m "An invisible fortune cookie?")))))
