(ns lazybot.plugins.markov
  (:use lazybot.registry
        [lazybot.plugins.login :only [when-privs]]
        [amalloy.utils :only [! keywordize verify trim-seq]]
        (amalloy.utils [transform :only [transform-if
                                         make-str make-kw make-int]]
                       [debug :only [?]]))
  (:require [clojure.contrib.string :as s :only [capitalize join]]
            [somnium.congomongo :as mongo :only [insert! destroy! fetch-one]])
  (:import java.util.regex.Pattern))

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

(defn is-private? [channel]
  (not= \# (first channel)))

(defn db-name [channel]
  (if (is-private? channel)
    "pmdb"
    channel))

;;; tuneable parameters
(def topic-weight 3/2)
(def min-topic-word-length 4)
(def topics-to-track 50)

;;; Constants to account for mongo making it impossible to tell keywords
;;; from strings. These can be more or less anything, but should not be any
;;; string that could be returned from tokenize, and they should not be
;;; changed without completely wiping the database - null pointer
;;; exceptions are likely to occur otherwise
(def start-sentence "##START##")
(def end-sentence "##END##")
(def break-token "##COMMA##")

;;; Parsing section
(defn word-char? [c]
  (and (char? c)
       (or (Character/isLetterOrDigit c)
           ((set "_-*'") c))))

(def break-str ",;:")
(def break-re (Pattern/compile (str "\\s*([" break-str "]+)")))
(def break-set (set break-str))
(def break-seq (seq break-str))
(defn rand-break [& _] (rand-nth break-seq))

(def semantic-break? break-set)

(defn sentence-terminator? [c]
  ((set ".?!") c))

(defn whitespace? [c]
  (and (char? c)
       (or (Character/isWhitespace c)
           ((set "\\+/<>|{}()[]\"") c))))

(defn ignore? [c]
  (not-any? #(% c)
            [word-char? semantic-break? sentence-terminator? whitespace?]))

(defn replace-with
  "Scans a seq of characters and/or keywords, replacing contiguous groups of
   objects satisfying pred with the result of (repl seq-of-objects). If repl is
   not a (fn) object, it is used as a literal replacement object."
  [pred repl elts]
  (let [repl (if (fn? repl)
               repl
               (constantly [repl]))]
    (->> elts
         (partition-by (comp boolean pred))
         (mapcat (fn [[x :as item]]
                   (if (pred x)
                     (repl item)
                     item))))))

;; TODO this is seriously disgusting and I'm pretty sure can be way better
(defn tokenize
  "Take an input string and split it into a seq of sentences. Each sentence
   will be further split into a seq of words."
  [msg]
  (->> msg
       .toLowerCase
       (remove ignore?)            ; will also convert from str to char seq
       (replace-with semantic-break? ::break)
       (replace-with sentence-terminator? ::sentence-sep)
       (replace-with whitespace? ::word-sep)
       (replace-with char? #(vector (apply str %)))
       (replace-with #{::word-sep} (constantly nil))
       (replace-with #{::break} (constantly [break-token]))
       (partition-by #{::sentence-sep})
       (remove #(every? keyword? %))))

(defn ngrams
  ([items]
     (ngrams 2 items))
  ([size items]
     (into {}
           (map (juxt butlast last)
                (partition size 1 items)))))

(defn links-in
  "Returns a seq of the touching pairs in a tokenized sentence, with
  special start and end markers added."
  [sentence]
  (ngrams sentence (concat [start-sentence]
                           sentence
                           [end-sentence])))

(defn kv-munge
  "Returns a function that operates on a link-map by applying kf and vf to
  each link in the tree."
  [kf vf]
  (fn [link-map]
    (into {} (for [[k v] link-map]
               [(kf k) (vf v)]))))

(defn mongoize
  "Convert a link map to the same format mongo uses. Will throw an
  exception if the supplied link map is not recognizable as a map."
  [x]
  {:pre [(every? string? (map first x))
         (every? integer? (map second x))]
   :post [(every? keyword? (map first %))
          (every? integer? (map second %))]}
  ((kv-munge make-kw make-str) x))

(defn demongoize
  "Convert a link map from mongo's keyword/string hybrid format to a
strings-only format for easier processing. Fails loudly if the supplied
link map is not in mongo format."
  [x] {:pre [(every? keyword? (map first x))]
       :post [(every? string? (map first %))
              (every? integer? (map second %))]}
  ((kv-munge make-str make-int) x))

(defn where-clause [chan word]
  {:chan.irc (:irc chan)
   :chan.channel (:channel chan)
   :word word})

(defn learn
  "Absorb a set of word pairs and add them to the supplied vocabulary."
  [vocabulary links]
  (let [chan (meta vocabulary)]
    (doseq [[word dest] links]
      (mongo/update!
       :markov (where-clause chan word)
       {:$inc {(str "links." dest) 1}}))))

;;; Storage and manipulation of the actual map

(def global-topics (atom (list)))

(defn mongo-vocab
  "Create a function that looks up its argument in the mongo database
  corresponding to the supplied channel. See also vocabulary."
  [irc channel]
  (let [chan (keywordize [irc channel])]
    (with-meta
      (memoize                          ; not that it really matters
       (fn [word]
         (let [res (mongo/fetch-one :markov :where
                                    (where-clause chan (make-str word)))]
           (if (seq res)
             (update-in res [:links] demongoize)
             res))))
      chan)))

(defn vocabulary
  "Look up the vocabulary function for the supplied context. A vocabulary
  function should accept a single string argument, and return a map of
  {word, frequency} pairs. Maintainers (ha, I wish) should call this
  more-abstract function in preference to the lower-level vocab functions
  like mongo-vocab, as this allows better pluggability."
  [bot irc channel]
  (mongo-vocab (:server @irc) channel))

(defn current-topics [bot irc channel]
  @global-topics)

(defn update-vocab!
  "Instruct the bot to learn a tokenized message."
  [bot irc channel tokens]
  (learn (vocabulary bot irc channel) (mapcat links-in tokens)))

(defn update-topics! [bot irc channel tokens]
  (swap! global-topics (comp #(take topics-to-track %)
                             distinct
                             (partial remove
                                      (comp #(> min-topic-word-length %)
                                            count))
                             #(into %1 (reverse (apply concat %2))))
         tokens))

(defn learn-message
  "Tell the bot it has received a message that it should tokenize, learn,
  and note as containing things the channel is currently 'interested in'."
  [bot irc channel msg]
  (let [tokens (tokenize msg)]
    (doseq [f [update-vocab! update-topics!]]
      (f bot irc channel tokens))))

;;; Sentence-creation section

(defn squish-breaks [str]
  (s/replace-re break-re "$1" str))

(defn interest-in [topics]
  (fn [[word weight]]
    [word (* weight
             (if (some #{word} topics)
               topic-weight
               1))]))

(defn pick-word
  "Given a vocabulary, a list of current interests, and a previous word,
  select the word to say next. Selects randomly from among all words it's
  ever seen after this one, giving preference to words used most often in
  this context, and to words that are currently topics of interest."
  [vocabulary topics word]
  (let [{nexts :links} (vocabulary word)
        [words freqs] (apply map vector (map (interest-in topics) nexts))
        topical-freqs (map (interest-in topics) freqs)
        idx (roulette-wheel freqs)
        res (nth words idx)]
    (verify (! #{end-sentence}) res)))

(defn build-sentence
  "Construct a sentence at random with the given vocabulary "
  [vocabulary topics]
  (str (->> start-sentence
            (iterate #(pick-word vocabulary topics %))
            rest
            trim-seq
            (map (transform-if #{break-token}
                               rand-break))
            (s/join " ")
            squish-breaks
            s/capitalize)
       "."))

(defn learn-url
  "Fetch the contents of a URL and learn it as if it had been pasted directly to the current channel. Admin only."
  [{:keys [bot com nick channel] :as com-m} url]
  (when-privs com-m :admin
   (str "I'd love to read " url ", but amalloy won't teach me how :(")))

(defn trim-addressee [msg]
  (s/replace-re #"^\S+:" "" msg))

;;; Plugin mumbo-jumbo

(defplugin :irc
  (:hook
   :on-message
   (fn [{:keys [com bot nick channel message]}]
     (when-not (or (= nick (:name com))
                   (is-command? message bot))
       (learn-message bot com (db-name channel) (trim-addressee message)))))

  (:cmd
   "Say something that seems to reflect what the channel is talking about."
   #{"markov" "thoughts?"}
   (fn [{:keys [bot com channel args] :as com-m}]
     (send-message com-m
                   (if-let [sub-cmd (first args)]
                     (let [sub-args (rest args)]
                       (case sub-cmd
                             "url" (s/join ";"
                                           (for [url sub-args]
                                             (learn-url com-m url)))
                             (str "I don't understand " sub-cmd)))
                     (->> (apply build-sentence
                                 (for [f [vocabulary current-topics]]
                                   (f bot com (db-name channel))))
                          (fn [])
                          (repeatedly)
                          (drop-while #(< (count %) 20))
                          (first))))))
  (:indexes [[:chan.channel :chan.irc :word] :unique true :force true]))
