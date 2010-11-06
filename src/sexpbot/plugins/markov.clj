(ns sexpbot.plugins.markov
  (:use sexpbot.respond)
  (:require [clojure.string :as string :only [join]])
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

;; Kinda annoying to type out all the time
(def ^{:arglists '([f])} ! complement)

(defn verify [pred x]
  (when (pred x)
    x))

(defn trim-seq [s]
  (take-while identity s))

(def topic-weight 3)

(defn current-topics [bot irc channel]
  ;; TODO: This is just a stub for dev/testing
  (set (map str
            '[clojure
              macros
              cheesecake])))

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