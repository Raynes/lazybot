(ns sexpbot.plugins.hello-world
  (:use sexpbot.registry))

(defplugin
  (:cmd
   "Say hi!"
   #{"hiworld"}
   (fn [com-m] (send-message com-m "Hello, World!"))))