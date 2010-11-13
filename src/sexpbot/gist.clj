(ns sexpbot.gist
  (:use [clj-github.gists :only [new-gist]])
  (:require [clojure.contrib.string :as string])
  (:import [java.io IOException]))

(defn word-wrap [str]
  (string/replace-re #"(.{50,70}[])}\"]*)\s+" "$1\n" str))

(defn split-str-at [len s]
  (map #(apply str %)
       ((juxt take drop) len s)))

(def gist-note "... ")
(def default-cap 300)

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
     (if (number? opt)
       (trim-with-gist opt "result.clj" "" s)
       (trim-with-gist default-cap opt "" s)))
  ([cap name s]
     (trim-with-gist cap name "" s))
  ([cap name gist-prefix s]
     (let [[before after] (split-str-at cap s)]
       (if-not (seq after)
         before
         (let [note (str gist-note
                         (try
                           (->> s
                                (str gist-prefix)
                                word-wrap
                                (new-gist {} name)
                                :repo
                                (str "http://gist.github.com/"))
                           (catch IOException e (str "failed to gist: "
                                                     (.getMessage e)))))]
           (str (string/take (- cap (count note)) s)
                note))))))
