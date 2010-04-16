(ns sexpbot.plugins.walton
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]))

(def wurl "http://getclojure.org:8080/examples/")

(defn construct-url [func] (str wurl func))

(defmethod respond :walton [{:keys [bot channel sender args]}]
  (ircb/send-message bot channel (->> args first construct-url (str sender ": "))))

(defplugin
  {"walton" :walton})