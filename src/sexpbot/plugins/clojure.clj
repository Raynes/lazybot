(ns sexpbot.plugins.clojure
  (:use clojure.stacktrace
	[clojail core testers]
	sexpbot.registry
    [sexpbot.plugins.shorturl :only [is-gd]]
    [sexpbot.gist :only [trim-with-gist]])
  (:require [clojure.string :as string :only [replace]])
  (:import java.io.StringWriter
           java.util.concurrent.TimeoutException
           (java.util.regex Pattern)))

(def sb (sandbox secure-tester))

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
   (special-form-anchor `~s)
   `(sfmsg "Special Form" (special-form-anchor '~s))
   (syntax-symbol-anchor `~s)
   `(sfmsg "Syntax Symbol" (syntax-symbol-anchor '~s))
   :else
   `(let [m# (-> ~s var meta)
          formatted# (when (:doc m#) (str (:arglists m#) "; " (.replaceAll (:doc m#) "\\s+" " ")))]
      (if (:macro m#) (str "Macro " formatted#) formatted#))))

(defn execute-text [bot-name user txt]
  (try
    (with-open [writer (StringWriter.)]
      (let [res (pr-str (sb (read-string txt) {#'*out* writer #'doc #'pretty-doc}))
            replaced (.replaceAll (str writer) "\n" " ")]
        (str "\u27F9 " (trim bot-name user txt (str replaced (when (= last \space) " ") res)))))
   (catch TimeoutException _ "Execution Timed Out!")
   (catch Exception e (str (root-cause e)))))

(def many (atom 0))

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
     (let [m (re-matcher pattern target)]
       (->> (repeatedly #(re-find m))
            (take-while identity)
            (map second)
            seq))))

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

(defn- eval-forms [bot-name user [form1 form2 :as forms]]
  (take max-embedded-forms
        (if-not form2
          [(execute-text bot-name user form1)]
          (map (fn [f]
                 (str
                  (let [trimmed (apply str (take 40 f))]
                    (if (> (count f) 40)
                      (str trimmed "... ")
                      trimmed)) " " (execute-text bot-name user f)))
               forms))))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [irc bot nick channel message]}]
     (.start
      (Thread.
       (fn []
         (if-let [evalp (-> @bot :config :eval-prefixes)]
           (when-let [sexps (what-to-eval bot channel message)]
             (if (< @many 3)
               (do
                 (try
                   (swap! many inc)
                   (doseq [sexp (eval-forms (:name @irc) nick sexps)]
                     (send-message irc bot channel sexp))
                   (finally (swap! many dec))))
               (send-message irc bot channel "Too much is happening at once. Wait until other operations cease.")))
           (throw (Exception. "Dude, you didn't set :eval-prefixes. I can't configure myself!"))))))))

  (:cmd
   "Link to the source code of a Clojure function or macro."
   #{"source"}
   (fn [{:keys [irc bot channel args]}]
     (if-let [line-url (get-line-url (first args))]
       (send-message irc bot channel (str (first args)  " is " line-url))
       (send-message irc bot channel "Source not found.")))))