(ns sexpbot.plugins.walton
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]))

(def wurl "http://getclojure.org:8080/examples/")

(defn construct-url [func] (str wurl func))

(defmethod respond :walton [{:keys [irc channel nick args]}]
  (ircb/send-message irc channel (->> args first construct-url (str nick ": "))))

(defplugin
  {"walton" :walton})