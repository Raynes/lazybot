(ns sexpbot.plugins.haskell
  (:use [sexpbot respond]
        [clojure.contrib.json :only [read-json]]
	[clojure-http.client :only [add-query-params]]
        [clojure.java.shell :only [sh]]
        [sexpbot.plugins.github :only [trim-with-gist]])
  (:require [clojure-http.resourcefully :as res]))

(def tryurl "http://tryhaskell.org/haskell.json")

(defn cull [js]
  (if-let [result (seq (:result js))] result (:error js)))

(def cap 300)

(defn trim [s]
  (trim-with-gist cap "output.hs" s))

(defn eval-haskell [expr]
  (->> (res/get (add-query-params tryurl {"method" "eval" "expr" expr})) 
       :body-seq
       first
       read-json
       cull
       (apply str)))

(defn mueval [expr]
  (:out (sh "mueval" "-e" expr)))

(defplugin
  (:cmd
   "Evaluates some Haskell code. Doesn't print error messages and uses the TryHaskell API."
   #{"tryhaskell"} 
   (fn [{:keys [irc bot channel args]}]
     (send-message irc bot channel (trim (str "=> " (eval-haskell (apply str (interpose " " args))))))))

  (:cmd
   "Evaluates Haskell code with mueval."
   #{"heval" "he"}
   (fn [{:keys [irc bot channel args]}]
     (let [result (mueval (apply str (interpose " " args)))]
       (send-message irc bot channel
                     (str "\u27F9 " (trim (if (> 200 (count result)) result (str result "..."))))))))

  (:cmd
   "Gets the type of an expression via GHC's :t."
   #{"htype" "ht"}
   (fn [{:keys [irc bot channel args]}]
     (send-message irc bot channel
                   (str "Type: " (->> args (interpose " ") (apply str) (str ":t ") (sh "ghc" "-e") :out))))))