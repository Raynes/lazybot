(ns lazybot.plugins.haskell
  (:require [lazybot.registry :refer [send-message defplugin]]
            [cheshire.core :refer [parse-string]]
            [clojure.java.shell :refer [sh]]
            [lazybot.paste :refer [trim-with-paste]]
            [clj-http.client :as http]))

(def tryurl "http://tryhaskell.org/haskell.json")

(defn cull [js]
  (or (seq (get js "result"))
      (get js "error")))

(def cap 300)

(defn trim [s]
  (trim-with-paste cap "Haskell" s))

(defn eval-haskell [expr]
  (->> (http/get tryurl {:query-params {"method" "eval" "expr" expr}})
       :body
       parse-string
       cull
       (apply str)))

(defn mueval [expr]
  (:out (sh "mueval" "-e" expr)))

(defn ghc-type [expr]
  (->> expr (str ":t ") (sh "ghc" "-e") :out (str "Type: ")))

(defn heval-cmd
  "Build a function suitable for use as a plugin's :cmd key, using the specified haskell evaluation function."
  [evaluator]
  (fn [{:keys [bot args] :as com-m}]
    (send-message
     com-m
     (str (get-in @bot [:config :prefix-arrow])
          (->> args
               (interpose " ")
               (apply str)
               evaluator)))))

(defplugin
  (:cmd
   "Evaluates some Haskell code. Doesn't print error messages and uses the TryHaskell API."
   #{"tryhaskell"} 
   (heval-cmd eval-haskell))

  (:cmd
   "Evaluates Haskell code with mueval."
   #{"heval" "he"}
   (heval-cmd mueval))

  (:cmd
   "Gets the type of an expression via GHC's :t."
   #{"htype" "ht"}
   (heval-cmd ghc-type)))
