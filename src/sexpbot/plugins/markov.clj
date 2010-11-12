(ns sexpbot.plugins.markov
  (:use sexpbot.respond
        [sexpbot.utilities :only [keywordize]])
  (:require [clojure.string :as string :only [join split replace]]
            [somnium.congomongo :as mongo :only [insert! destroy! fetch-one]])
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

(defmacro and-print
  "A useful debugging tool when you can't figure out what's going on:
  wrap a form with and-print, and the form will be printed alongside
  its result. The result will still be passed along."
  [val]
  `(let [x# ~val]
     (println '~val "is" x#)
     x#))

(defn transform-if
  ([pred f]
     (transform-if pred f identity))
  ([pred f f-not]
     (fn [x]
       (if (pred x) (f x) (f-not x)))))

(def make-str (transform-if keyword? name str))
(def make-kw keyword)
(def make-int (transform-if string? #(Integer/parseInt %)))

;;; tuneable parameters
(def topic-weight 3)
(def min-topic-word-length 4)
(def topics-to-track 50)

(def start-sentence "##START##")
(def end-sentence "##END##")

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
             (concat [start-sentence]
                     sentence
                     [end-sentence])))

(defn kv-munge [kf vf]
  (fn [link-map]
    (into {} (for [[k v] link-map]
               [(kf k) (vf v)]))))

(defn mongoize [x] {:pre [(every? string? (map first x))
                          (every? integer? (map second x))]
                    :post [(every? keyword? (map first %))
                           (every? integer? (map second %))]}
  ((kv-munge make-kw make-str) x))

(defn demongoize [x] {:pre [(every? keyword? (map first x))]
                      :post [(every? string? (map first %))
                             (every? integer? (map second %))]}
  ((kv-munge make-str make-int) x))

(defn learn [vocabulary links]
  (reduce (fn [vocab link] {:pre [(every? string? link)
                                  (seq link)]}
            (update-in vocab link (fnil inc 0)))
          vocabulary
          links))

;;; Storage and manipulation of the actual map

(def global-topics (atom (list)))

(defn mongo-vocab [irc channel]
  (fn [word]
    (let [res (mongo/fetch-one :markov :where
                               {:chan (keywordize [irc channel])
                                :word (make-str word)})]
      (if (seq res)
        (update-in res [:links] demongoize)
        res))))

(defn vocabulary [bot irc channel]
  (mongo-vocab (:server @irc) channel))

(defn current-topics [bot irc channel]
  @global-topics)

(defn update-vocab! [bot irc channel tokens]
  (let [vocab (vocabulary bot irc channel)
        irc (:server @irc)
        chan (keywordize [irc channel])
        chain (mapcat links-in tokens)
        db-links (map vocab (distinct (map first chain)))
        entries (into {} (filter first
                                 (map (juxt :word :links)
                                      db-links)))
        links (learn entries chain)]
    (doseq [word (keep first links)]
      (let [links (get links word)
            key (keywordize [chan word])]
        (mongo/destroy! :markov key)
        (mongo/insert! :markov
                       (assoc key :links links))))))

(defn update-topics! [bot irc channel tokens]
  (swap! global-topics (comp #(take topics-to-track %)
                             distinct
                             (partial remove
                                      (comp #(> min-topic-word-length %)
                                            count))
                             #(into %1 (reverse (apply concat %2))))
         tokens))

(defn learn-message [bot irc channel msg]
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
  (let [{nexts :links} (vocabulary word)
        [words freqs] (apply map vector (map (interest-in topics) nexts))
        topical-freqs (map (interest-in topics) freqs)
        idx (roulette-wheel freqs)
        res (nth words idx)]
    (verify (! #{end-sentence}) res)))

(defn build-sentence [vocabulary topics]
  (str (->> start-sentence
            (iterate #(pick-word vocabulary topics %))
            rest
            trim-seq
            (string/join " "))
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
   (fn [{:keys [bot irc channel]}]
     (send-message irc bot channel (apply build-sentence
                                          (map #(% bot irc channel)
                                               [vocabulary current-topics]))))))