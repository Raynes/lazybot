(ns sexpbot.plugins.markov
  (:use sexpbot.respond)
  (:require [clojure.string :as string :only [join split replace]])
  (:import))

;; Stolen from:
;; http://github.com/liebke/incanter/blob/master/modules/incanter-core/src/incanter/distributions.clj,
;; rather than depending on the whole massive incanter project
(defn roulette-wheel
  "Perform a roulette wheel selection given a list of frequencies"
  [freqs]
  (let [nfreqs (count freqs)
        tot (reduce + freqs)]
    (if (= tot 0)
      nil
      (let [dist (map #(/ % tot) freqs)
            rval (double (rand))]
        (loop [acc 0, i 0]
          (let [lb acc, ub (+ acc (nth dist i))]
            (cond (>= (+ i 1) nfreqs) i
                  (and (>= rval lb) (< rval ub)) i
                  :else (recur ub (+ i 1)))))))))

;;; Things that are just annoying to type out all the time
(def ^{:arglists (:arglists (meta #'complement))}
  ! complement)

(defn verify [pred x]
  (when (pred x)
    x))

(defn trim-seq [s]
  (take-while identity s))

;;; tuneable parameters
(def topic-weight 3)
(def min-topic-word-length 4)
(def topics-to-track 50)

;;; Storage and manipulation of the actual map
(defn vocabulary [bot irc channel]
  ;; TODO: Stubbed
  {::start-sentence {"clojure" 3
                     "my" 1}
   "clojure" {"is" 4
              ::end-sentence 1}
   "is" {"awesome" 7
         "clojure" 1}
   "awesome" {::end-sentence 1}
   "my" {"favorite" 1}
   "favorite" {"language" 1}
   "language" {"is" 1}})

(defn current-topics [bot irc channel]
  ;; TODO: This is just a stub for dev/testing
  (set (map str
            '[clojure
              macros
              cheesecake])))

;;; Sentence-creation section
(defn interest-in [topics]
  (fn [[word weight]]
    [word (* weight
             (if (topics word)
               topic-weight
               1))]))

(defn pick-word [vocabulary topics word]
  (let [nexts (get vocabulary word)
        [words freqs] (apply map vector (map (interest-in topics) nexts))
        topical-freqs (map (interest-in topics) freqs)
        idx (roulette-wheel freqs)
        res (nth words idx)]
    (verify (! #{::end-sentence}) res)))

(defn build-sentence [vocabulary topics]
  (str (->> ::start-sentence
            (iterate #(pick-word vocabulary topics %))
            rest
            trim-seq
            (string/join " "))
       "."))

;;; Parsing section
(defn word-char? [c]
  (or (Character/isLetterOrDigit c)
      ((set "_-*'") c)))

(defn sentence-terminator? [c]
  ((set ".?!:;") c))

(defn whitespace? [c]
  (or (Character/isWhitespace c)
      ((set "\\,+/<>|{}()[]\"") c)))

(defn ignore? [c]
  (not-any? #(%1 c) [word-char? sentence-terminator? whitespace?]))

(defn replace-with
  "Scans a seq of characters and/or keywords, replacing contiguous groups of
  objects satisfying pred with the result of (repl seq-of-objects). If repl is
  not a (fn) object, it is used as a literal replacement object."
  [pred repl elts]
  (let [repl (if (fn? repl)
               repl
               (constantly [repl]))]
    (->> elts
         (partition-by #(boolean (pred %)))
         (mapcat (fn [[x :as item]]
                   (if (pred x)
                     (repl item)
                     item))))))

(defn tokenize
  "Take an input string and split it into a seq of sentences. Each sentence will
  be further split into a seq of words."
  [msg]
  (->> msg
       .toLowerCase
       (remove ignore?) ; will also convert from str to char seq
       (replace-with whitespace? ::word-sep) ; placeholder while working with
                                             ; chars instead of strs
       (replace-with sentence-terminator? ::sentence-sep)
       (replace-with char? #(vector (apply str %)))
       (replace-with #{::word-sep} (constantly nil))
       (partition-by #{::sentence-sep})
       (remove #(every? keyword? %))))

;;; Plugin mumbo-jumbo
(defplugin
  (:hook
   :on-message
   (fn [{:keys [irc bot nick channel message]}]
     (comment TODO - add stuff to the database, and maybe
              add forms of address other than $cmd, eg "sexpbot: thoughts?")))

  (:cmd
   "Say something that seems to reflect what the channel is talking about."
   #{"markov"}
   (fn [{:keys [irc bot channel args]}]
     ;; TODO - do something.
     (send-message irc bot channel (first args)))))