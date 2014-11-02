(ns lazybot.plugins.hello-world
  (:require [lazybot.registry :as registry]))

(registry/defplugin
  (:cmd
   "Say hi!"
   #{"hiworld"}
   (fn [com-m] (registry/send-message com-m "Hello, World!"))))
