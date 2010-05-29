(ns sexpbot.plugins.haskell
  (:use [sexpbot respond])
  (:require [org.danlarkin.json :as json]
	    [clojure-http.resourcefully :as res]
	    [irclj.irclj :as ircb]))

(def tryurl "http://tryhaskell.org/haskell.json")

(defn cull [js]
  (if-let [result (seq (:result js))] result (:error js)))

(defn eval-haskell [expr]
  (->> (res/get tryurl {} {"method" "eval" "expr" expr}) 
       :body-seq
       first
       json/decode-from-str
       cull
       (apply str)))

(defplugin
  (:heval 
   "Evaluates some Haskell code. Doesn't print error messages and uses the TryHaskell API."
   ["heval"] 
   [{:keys [irc channel nick args]}]
   (ircb/send-message irc channel (str "\"" (eval-haskell (apply str (interpose " " args))) "\""))))