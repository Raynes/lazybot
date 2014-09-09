(ns lazybot.plugins.hodor
  (:require [lazybot.registry :as reg]))

(reg/defplugin
  (:cmd
   "hodor"
   #{"hodor"}
   (fn [com-m] (reg/send-message com-m "hodor"))))
