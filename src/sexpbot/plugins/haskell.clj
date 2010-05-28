(ns sexpbot.plugins.haskell
  (:use [sexpbot respond])
  (:require [org.danlarkin.json :as json]
	    [clojure-http.resourcefully :as res]
	    [irclj.irclj :as ircb]))

(def tryurl "http://tryhaskell.org/haskell.json?method=eval")

(defn eval-haskell [expr]
  (-> (res/get tryurl {} {"expr" expr}) 
      :body-seq
      first
      json/decode-from-str
      :result))

(defplugin
  (:heval 
   "Evaluates some Haskell code. Doesn't print error messages and uses the TryHaskell API."
   ["heval"] 
   [{:keys [irc channel nick args]}]
   (ircb/send-message irc channel 
		      (str nick ": " (eval-haskell (apply str (interpose " " args)))))))