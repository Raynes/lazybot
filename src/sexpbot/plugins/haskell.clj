(ns sexpbot.plugins.haskell
  (:use [sexpbot respond])
  (:require [org.danlarkin.json :as json]
	    [com.twinql.clojure.http :as http]
	    [irclj.irclj :as ircb]))

(def tryurl "http://tryhaskell.org/haskell.json?method=eval")

(defn eval-haskell [expr]
  (-> (http/get (java.net.URI. tryurl) :query {:expr expr} :as :string) 
      :content
      json/decode-from-str
      :result))

(defmethod respond :heval [{:keys [bot channel sender args]}]
  (ircb/send-message bot channel 
		     (str sender ": " (eval-haskell (apply str (interpose " " args))))))

(defplugin
  {"heval" :heval})