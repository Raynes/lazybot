(ns sexpbot.plugins.haskell
  (:use [sexpbot respond]
        [clojure.contrib.json :only [read-json]]
	[clojure-http.client :only [add-query-params]])
  (:require [clojure-http.resourcefully :as res]))

(def tryurl "http://tryhaskell.org/haskell.json")

(defn cull [js]
  (if-let [result (seq (:result js))] result (:error js)))

(defn eval-haskell [expr]
  (->> (res/get (add-query-params tryurl {"method" "eval" "expr" expr})) 
       :body-seq
       first
       read-json
       cull
       (apply str)))

(defplugin
  (:cmd
   "Evaluates some Haskell code. Doesn't print error messages and uses the TryHaskell API."
   #{"heval"} 
   (fn [{:keys [irc bot channel nick args]}]
     (send-message irc bot channel (str "=> " (eval-haskell (apply str (interpose " " args))))))))