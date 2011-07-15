(ns lazybot.plugins.hello-world
  (:use lazybot.registry))

(defplugin
  (:cmd
   "Say hi!"
   #{"hiworld"}
   (fn [com-m] (send-message com-m "Hello, World!"))))