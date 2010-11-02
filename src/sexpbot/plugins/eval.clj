(ns sexpbot.plugins.eval
  (:use net.licenser.sandbox
	clojure.stacktrace
	[net.licenser.sandbox tester matcher]
	sexpbot.respond
	[clj-github.gists :only [new-gist]])
  (:import java.io.StringWriter
           java.util.concurrent.TimeoutException
           (java.util.regex Pattern)))

(enable-security-manager)

(def sandbox-tester
     (new-tester
      (whitelist)
      (blacklist
       (function-matcher 'alter-var-root 'intern 'def 'eval 'catch 'load-string 'load-reader 'clojure.core/addMethod))))

#_(def sandbox-tester
     (extend-tester secure-tester 
		    (whitelist 
		     (function-matcher '*out* 'println 'print 'pr 'prn 'var 'print-doc 'doc 'throw
                                       'def 'defn 'promise 'deliver 'future-call 'special-form-anchor
                                       'syntax-symbol-anchor 'sfmsg 'unquote)
                     (namespace-matcher 'clojure.string 'clojure.repl)
		     (class-matcher java.io.StringWriter java.net.URL java.net.URI
                                    java.util.TimeZone java.lang.System))))

(def my-obj-tester
     (extend-tester default-obj-tester
		    (whitelist)))

(def sc (stringify-sandbox (new-sandbox-compiler :tester sandbox-tester 
						 :timeout 10000 
					  	 :object-tester my-obj-tester
                                                 :remember-state 5)))

(def cap 300)

(defn trim [s]
  (let [res (apply str (take cap s))
	rescount (count res)]
    (if (= rescount cap) 
      (str res "... "
           (when (> (count s) cap)
             (try
               (str "http://gist.github.com/" (:repo (new-gist {} "output.clj" s)))
               (catch java.io.IOException _ nil))))
      res)))

;(defmacro defn2 [name & body] `(def ~name (fn ~name ~@body)))

(defn sfmsg [t anchor] (str t ": Please see http://clojure.org/special_forms#" anchor))

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

(defn execute-text [txt]
  (try
    (with-open [writer (StringWriter.)]
      (binding [doc #'pretty-doc]
        (let [res (pr-str ((sc txt) {'*out* writer}))
              replaced (.replaceAll (str writer) "\n" " ")]
          (str "\u27F9 " (trim (str replaced (when (= last \space) " ") res))))))
   (catch TimeoutException _ "Execution Timed Out!")
   (catch SecurityException e (str (root-cause e)))
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

(defn- eval-forms [[form1 form2 :as forms]]
  (take max-embedded-forms
        (if-not form2
          [(execute-text form1)]
          (map (fn [f]
                 (str
                  (let [trimmed (apply str (take 40 f))]
                    (if (> (count f) 40)
                      (str trimmed "... ")
                      trimmed)) " " (execute-text f)))
               forms))))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [irc bot channel message]}]
     (.start
      (Thread.
       (fn []
         (if-let [evalp (-> @bot :config :eval-prefixes)]
           (when-let [sexps (what-to-eval bot channel message)]
             (if (< @many 3)
               (do
                 (try
                   (swap! many inc)
                   (doseq [sexp (eval-forms sexps)]
                     (send-message irc bot channel sexp))
                   (finally (swap! many dec))))
               (send-message irc bot channel "Too much is happening at once. Wait until other operations cease.")))
           (throw (Exception. "Dude, you didn't set :eval-prefixes. I can't configure myself!")))))))))