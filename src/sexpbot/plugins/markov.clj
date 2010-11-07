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

(defn and-print [val]
  (println val)
  val)

;;; tuneable parameters
(def topic-weight 3)
(def min-topic-word-length 4)
(def topics-to-track 50)

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
       (remove ignore?)                 ; will also convert from str to char seq
       (replace-with whitespace? ::word-sep) ; placeholder while working with
                                        ; chars instead of strs
       (replace-with sentence-terminator? ::sentence-sep)
       (replace-with char? #(vector (apply str %)))
       (replace-with #{::word-sep} (constantly nil))
       (partition-by #{::sentence-sep})
       (remove #(every? keyword? %))))

(defn links-in [sentence]
  (partition 2 1
             (concat [::start-sentence]
                     sentence
                     [::end-sentence])))

(defn learn [vocabulary links]
  (reduce (fn [vocab link]
            (update-in vocab link (fnil inc 0)))
          vocabulary
          links))

(defn look-and-learn [vocabulary tokens]
  (learn vocabulary (mapcat links-in tokens)))

;;; Storage and manipulation of the actual map

;; TODO This needs to be localized per-irc, maybe per-channel; it should also be
;; persisted to mongo, but I need some usage data to figure out what the
;; retention policy should be.
(def global-vocabulary (atom {::start-sentence {"clojure" 3
                                                "my" 1}
                              "clojure" {"is" 4
                                         ::end-sentence 1}
                              "is" {"awesome" 7
                                    "clojure" 1}
                              "awesome" {::end-sentence 1}
                              "my" {"favorite" 1}
                              "favorite" {"language" 1}
                              "language" {"is" 1}}))
(def global-topics (atom (map str
                              '[clojure
                                macros
                                cheesecake])))

(defn vocabulary [bot irc channel]
  @global-vocabulary)

(defn current-topics [bot irc channel]
  @global-topics)

(defn update-vocab! [bot irc channel tokens]
  (swap! global-vocabulary look-and-learn tokens))

(defn update-topics! [bot irc channel tokens]
  (swap! global-topics (comp #(take topics-to-track %)
                             #(apply concat %1 %2))
         tokens))

(defn learn-message [bot irc channel msg]
  (println 'learning msg)
  (let [tokens (tokenize msg)]
    (update-vocab! bot irc channel tokens)
    (update-topics! bot irc channel tokens)))

;;; Sentence-creation section
(defn interest-in [topics]
  (fn [[word weight]]
    [word (* weight
             (if (some #{word} topics)
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
            (string/join " ")
            and-print)
       "."))

;;; Plugin mumbo-jumbo

;; TODO add forms of address other than $markov, eg "sexpbot: thoughts?"
(defplugin
  (:hook
   :on-message
   (fn [{:keys [irc bot nick channel message]}]
     (when-not (or (= nick (:name irc))
                   (pos? (.indexOf message "markov")))
       (learn-message bot irc channel message))))

  (:cmd
   "Say something that seems to reflect what the channel is talking about."
   #{"markov"}
   (fn [{:keys [irc bot channel]}]
     ;; TODO - do something.
     (send-message irc bot channel (apply build-sentence
                                          (map #(% irc bot channel)
                                               [vocabulary current-topics]))))))