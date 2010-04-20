(ns sexpbot.plugins.walton
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb])
  (:use walton.core))

(background-init-walton)

(def wurl "http://getclojure.org:8080/examples/")

(defn construct-url [func] (str wurl func))

(defmethod respond :walton [{:keys [irc channel nick args]}]
  (ircb/send-message irc channel (->> args first construct-url (str nick ": "))))

(defmethod respond :example [{:keys [irc channel args]}]
  (let [[example result] (walton (first args))]
    (ircb/send-message irc channel (str example " => " result))))

(defplugin
  {"walton"  :walton
   "example" :example})