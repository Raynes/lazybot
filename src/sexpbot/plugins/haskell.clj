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

(defmethod respond :heval [{:keys [irc channel nick args]}]
  (ircb/send-message irc channel 
		     (str nick ": " (eval-haskell (apply str (interpose " " args))))))

(defplugin
  {"heval" :heval})