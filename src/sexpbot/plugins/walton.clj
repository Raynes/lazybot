(ns sexpbot.plugins.walton
  (:use [sexpbot respond commands]))

(def wurl "http://getclojure.org:8080/examples/")

(defn construct-url [func] (str wurl func))

(defmethod respond :walton [{:keys [bot channel sender args]}]
  (.sendMessage bot channel (->> args first construct-url (str sender ": "))))

(defmodule :walton
  {"walton" :walton})