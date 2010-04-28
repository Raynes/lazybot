(ns sexpbot.plugins.walton
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb])
  (:use walton.core))

(background-init-walton)

(def wurl "http://getclojure.org:8080/examples/")

(defn construct-url [func] (str wurl func))

(defplugin 
  (:walton 
   "Links to a getclojure page with examples for the function you specify."
   ["walton"]
   [{:keys [irc channel nick args]}]
   (ircb/send-message irc channel (->> args first construct-url (str nick ": "))))
  
  (:example 
   "Prints a random example of a function."
   ["example"] 
   [{:keys [irc channel args]}]
   (let [[example result] (walton (first args))]
     (ircb/send-message irc channel (str example " => " result)))))