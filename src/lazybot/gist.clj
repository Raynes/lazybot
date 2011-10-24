(ns lazybot.gist
  (:use [clj-github.gists :only [new-gist]]
        [lazybot.utilities :only [trim-string]])
  (:require [clojure.string :as string])
  (:import [java.io IOException]))

(defn word-wrap [str]
  (string/replace str #"(.{50,70}[])}\"]*)\s+" "$1\n"))

(def gist-note "... ")
(def default-cap 300)

(defn gist [name s]
  (str "https://gist.github.com/" (:repo (new-gist name s))))

(defn trim-with-gist
  "Trims the input string to a maximum of cap characters; if any
trimming is done, then a gist will be created to hold the entire
string, and the returned string will end with a link to that
gist. Prepends gist-prefix to the gist (if applicable), but not to the
result string - use this to give the gist additional context that is
not necessary in the result."
  ([s]
     (trim-with-gist default-cap "result.clj" "" s))
  ([opt s]
   (apply trim-with-gist
          (if (number? opt)
            [opt "result.clj" "" s]
            [default-cap opt "" s])))
  ([cap name s]
     (trim-with-gist cap name "" s))
  ([cap name gist-prefix s]
     (trim-string cap
                  (fn [s]
                    (str gist-note
                         (try
                           (->> s
                                (str gist-prefix)
                                word-wrap
                                (gist name))
                           (catch IOException e (str "failed to gist: "
                                                     (.getMessage e))))))
                  s)))