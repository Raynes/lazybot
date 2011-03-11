(ns sexpbot.plugins.clojure
  (:use clojure.stacktrace
	(clojail testers core) 
	sexpbot.registry
        clojure.contrib.logging
        [sexpbot.utilities :only [verify transform-if on-thread]]
        [sexpbot.plugins.shorturl :only [is-gd]]
        [sexpbot.gist :only [trim-with-gist]]
        [name.choi.joshua.fnparse :only [rule-match term failpoint alt complex rep*]])
  (:require [clojure.string :as string :only [replace]]
            ; these requires are for findfn
            clojure.string
            clojure.set
            clojure.contrib.string)
  (:import java.io.StringWriter
           java.util.concurrent.TimeoutException
           java.util.regex.Pattern
           clojure.lang.LispReader$ReaderException))


(def eval-tester secure-tester)
(def sb (sandbox eval-tester :transform pr-str))

(def cap 300)

(defn trim [bot-name user expression s]
  (trim-with-gist
    cap
    "result.clj" 
    (str "<" user "> " expression "\n<" bot-name "> \u27F9 ")
    s))

;(defmacro defn2 [name & body] `(def ~name (fn ~name ~@body)))

(defn sfmsg [t anchor] (str t ": Please see http://clojure.org/special_forms#" anchor))

(defn get-line-url [s]
  (let [s-meta (try (-> s symbol resolve meta) (catch Exception _ nil))
        ns-str (str (:ns s-meta))]
    (when-let [line (:line s-meta)]
      (is-gd
       (if-not (= "clojure.core" ns-str)
         (str "https://github.com/clojure/clojure-contrib/tree/1.2.x/src/main/clojure/"
              (:file s-meta) "#L" line)
         (str "https://github.com/clojure/clojure/blob/1.2.x/src/clj/clojure/core.clj#L" line))))))

(defmacro pretty-doc [s]
  (cond
   (special-form-anchor s)
   `(sfmsg "Special Form" (special-form-anchor '~s))
   (syntax-symbol-anchor s)
   `(sfmsg "Syntax Symbol" (syntax-symbol-anchor '~s))
   :else
   `(let [[a# m# d#] (-> ~s var meta ((juxt :arglists :macro :doc)))
          d# (when d#
               (string/replace d# #"\s+" " "))]
      (str (when m# "Macro ") a# "; " d#))))

;; fix-paren parser

(def tokenize (partial re-seq #"\\[\[\](){}]|\"(?:\\.|[^\"])*\"|[\[\](){}]|[^\[\](){}]+"))

(def initial-state (comp (partial array-map :remainder) tokenize))

(def brackets {"[" "]" "(" ")" "{" "}"})

(def opening (term (set (keys brackets))))

(def closing (failpoint (term (set (vals brackets)))
                        vector))

(def content (term (complement (set (flatten (seq brackets))))))

(def expression (alt (complex [open opening
                               body (rep* expression)
                               _    closing]                                
                              (str open (apply str body) (brackets open)))
                     content))

(def fix-parens (comp (partial rule-match expression (constantly nil) (constantly nil))
                      initial-state))

(defn safe-read-with-paren-fix
  [txt]
  (try (safe-read txt)
       (catch java.lang.RuntimeException e
         (if (= (.getMessage (.getCause e))
                "EOF while reading")
           (safe-read (fix-parens txt))
           (throw e)))))

(defn execute-text [bot-name user txt protocol]
  (try
    (with-open [writer (StringWriter.)]
      (let [res (sb (safe-read-with-paren-fix txt) {#'*out* writer #'doc #'pretty-doc})
            replaced (string/replace (str writer) "\n" " ")
            result (str replaced (when (= last \space) " ") res)
            twitter? (= protocol :twitter)]
        (str (when-not twitter? "\u27F9 ")
             (if twitter?
               result
               (trim bot-name user txt result)))))
   (catch TimeoutException _ "Execution Timed Out!")
   (catch Exception e (str (root-cause e)))))

(def tasks (atom [0]))

(defn first-object [s]
  (when (seq s)
    (try
      ((transform-if coll? pr-str (constantly nil))
       (safe-read s))
      (catch Exception _))))

(defmulti find-eval-request
  "Search a target string for eval requests.
Return a seq of strings to be evaluated. Usually this will be either nil or a one-element list, but it's possible for users to request evaluation of multiple forms with embedded specifiers, in which case it will be longer."
  {:arglists '([matcher target])}
  (comp class first list))

(defmethod find-eval-request String
  ([search target]
     (when (.startsWith target search)
       [(apply str (drop (count search) target))])))

(defmethod find-eval-request Pattern
  ([pattern target]
     (->> (re-seq pattern target)
          (keep (comp first-object second))
          seq)))

(defn- eval-config-settings [bot]
  (let [config-setting (-> @bot :config (get :eval-prefixes
                                             {:defaults #{}}))]
    (if (vector? config-setting)
      {:defaults (set config-setting)}      ; backwards compatible
      config-setting)))

(defn- default-prefixes [bot]
  (:defaults (eval-config-settings bot)))

;; Make sure Pattern objects show up first
(defn- pattern-comparator [a b]
  (let [ac (class a)
	bc (class b)]
    (if (= (= ac Pattern)
	   (= bc Pattern))
      (compare (str a) (str b))
      (if (= ac Pattern)
	-1
	1))))

(defn- eval-exceptions [bot channel]
  (set (get (eval-config-settings bot)
            channel
            [])))

(defn- what-to-eval [bot channel message]
  (let [candidates (default-prefixes bot)
        exceptions (eval-exceptions bot channel)
        patterns (sort pattern-comparator
		       (remove exceptions candidates))]
    (first (keep #(find-eval-request % message)
                 patterns))))

(def max-embedded-forms 3)

(defn- eval-forms [bot-name user protocol [form1 form2 :as forms]]
  (take max-embedded-forms
        (if-not form2
          [(execute-text bot-name user form1 protocol)]
          (map (fn [f]
                 (str
                  (let [trimmed (apply str (take 40 f))]
                    (if (> (count f) 40)
                      (str trimmed "... ")
                      trimmed))
                  " " (execute-text bot-name user f protocol)))
               forms))))

(def findfn-ns-set
     (map the-ns '#{clojure.core clojure.set clojure.string
                    clojure.contrib.string}))

(defn fn-name [var]
  (apply symbol (map str
                     ((juxt (comp ns-name :ns)
                            :name)
                      (meta var)))))

(defn find-fn
  [out & in]
  (debug (str "out:[" out "], in[" in "]"))
  (map fn-name
       (filter
        (fn [x]
          (try
            (thunk-timeout
             #(= out
                 (binding [*out* (java.io.StringWriter.)]
                   (apply
                    (doto (if (-> x meta :macro)
                            (fn [& args]
                              (eval `(~x ~@args))) ; args and x already vetted
                            x)
                      (->> (str "Trying ")
                           debug))
                    in)))
             50 :ms eagerly-consume)
            (catch Throwable _ false)))
        (remove (comp eval-tester
                      :name
                      meta)
                (map second (mapcat ns-publics findfn-ns-set))))))

(defn read-findfn-args
  "From an input string like \"in1 in2 in3 out\", return a vector of [out
  in1 in2 in3], for use in findfn."
  [argstr]
  (apply concat
         ((juxt (comp list last) butlast)
          (with-in-str argstr
            (let [sentinel (Object.)]
              (doall
               (take-while (complement #{sentinel})
                           (repeatedly
                            #(try
                               (safe-read)
                               (catch LispReader$ReaderException _
                                 sentinel))))))))))

(defn findfn-pluginfn [argstr]
  (try
    (let [argvec (vec (read-findfn-args argstr))
          _ (sb argvec)       ; a lame hack to get sandbox
                              ; guarantees on eval-ing the user's args
          user-args (eval argvec)]
      (->> user-args (apply find-fn) vec str trim-with-gist))
    (catch Throwable e
      (.getMessage e))))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [com bot nick channel message] :as com-m}]
     (if-let [evalp (-> @bot :config :eval-prefixes)]
       (when-let [sexps (what-to-eval bot channel message)]
         (if-not (second (swap! tasks (fn [[pending]]
                                        (if (< pending 3)
                                          [(inc pending) true]
                                          [pending false]))))
           (send-message com-m "Too much is happening at once. Wait until other operations cease.")
           (on-thread
            (try
              (doseq [msg (eval-forms (:name @com) nick (:protocol @bot) sexps)]
                (send-message com-m msg))
              (finally (swap! tasks (fn [[pending]]
                                      [(dec pending)])))))))
       (throw (Exception. "Dude, you didn't set :eval-prefixes. I can't configure myself!")))))

  (:cmd
   "Link to the source code of a Clojure function or macro."
   #{"source"}
   (fn [{:keys [args] :as com-m}]
     (send-message com-m
                   (if-let [line-url (get-line-url (first args))]
                     (str (first args)  " is " line-url)
                     "Source not found."))))

  (:cmd
   "Finds the clojure fns which, given your input, produce your output."
   #{"findfn"}
   (fn [{:keys [args] :as com-m}]
     (send-message com-m (findfn-pluginfn (string/join " " args))))))
