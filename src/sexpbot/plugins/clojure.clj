(ns sexpbot.plugins.clojure
  (:use clojure.stacktrace
	(clojail testers core) 
	sexpbot.registry
        clojure.contrib.logging
        [sexpbot.utilities :only [verify transform-if on-thread]]         
        [sexpbot.plugins.shorturl :only [is-gd]]
        [sexpbot.gist :only [trim-with-gist]])
  (:require [clojure.string :as string :only [replace]]
            ; these requires are for findfn
            clojure.string
            clojure.set
            clojure.contrib.string)
  (:import java.io.StringWriter
           java.util.concurrent.TimeoutException
           java.util.regex.Pattern))

(def eval-tester secure-tester)
(def sb (sandbox eval-tester))

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

(defn execute-text [bot-name user txt protocol]
  (try
    (with-open [writer (StringWriter.)]
      (let [res (pr-str (sb (read-string txt) {#'*out* writer #'doc #'pretty-doc}))
            replaced (string/replace (str writer) "\n" " ")
            result (str replaced (when (= last \space) " ") res)
            twitter? (= protocol :twitter)]
        (str (when-not twitter? "\u27F9 ")
             (if twitter?
               result
               (trim bot-name user txt result)))))
   (catch TimeoutException _ "Execution Timed Out!")
   (catch Exception e (str (root-cause e)))))

(def many (atom 0))

(defn first-object [s]
  (when (seq s)
    (binding [*read-eval* false]
      (try
        ((transform-if coll? pr-str (constantly nil))
         (read-string s))
        (catch Exception _)))))

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
                      trimmed)) " " (execute-text bot-name user f protocol)))
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
             50 :ms)
            (catch Throwable _ false)))
        (remove (comp eval-tester
                      :name
                      meta)
                (map second (mapcat ns-publics findfn-ns-set))))))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [com bot nick channel message] :as com-m}]
     (on-thread
      (if-let [evalp (-> @bot :config :eval-prefixes)]
        (when-let [sexps (what-to-eval bot channel message)]
          (if (< @many 3)
            (do
              (try
                (swap! many inc)
                (doseq [sexp (eval-forms (:name @com) nick (:protocol @bot) sexps)]
                  (send-message com-m sexp))
                (finally (swap! many dec))))
            (send-message com-m "Too much is happening at once. Wait until other operations cease.")))
        (throw (Exception. "Dude, you didn't set :eval-prefixes. I can't configure myself!"))))))

  (:cmd
   "Link to the source code of a Clojure function or macro."
   #{"source"}
   (fn [{:keys [com bot channel args] :as com-m}]
     (if-let [line-url (get-line-url (first args))]
       (send-message com-m (str (first args)  " is " line-url))
       (send-message com-m "Source not found."))))

  (:cmd
   "Finds the clojure fns which, given your input, produce your output."
   #{"findfn"}
   (fn [{:keys [bot args] :as com-m}]
     (let [[user-in user-out :as args]
           ((juxt butlast last)
            (with-in-str (string/join " " args)
              (doall
               (take-while (complement #{::done})
                           (repeatedly
                            #(try
                               (read)
                               (catch Throwable _ ::done)))))))]
       (send-message com-m (-> `(find-fn ~user-out ~@user-in)
                               sb vec str trim-with-gist))))))