(ns sexpbot.plugins.eval
  (:use net.licenser.sandbox
	clojure.stacktrace
	[net.licenser.sandbox tester matcher]
	sexpbot.respond
	[clj-github.gists :only [new-gist]])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(enable-security-manager)

(def sandbox-tester
     (extend-tester secure-tester 
		    (whitelist 
		     (function-matcher '*out* 'println 'print 'pr 'prn 'var 'print-doc 'doc 'throw
                                       'def 'promise 'deliver 'future-call)
                     (namespace-matcher 'clojure.string)
		     (class-matcher java.io.StringWriter java.net.URL java.net.URI
                                    java.util.TimeZone java.lang.System))))

(def my-obj-tester
     (extend-tester default-obj-tester
		    (whitelist
		     (class-matcher java.io.StringWriter String Byte Character StrictMath StringBuffer
				    java.net.URL java.net.URI java.util.TimeZone java.lang.System))))

(def sc (stringify-sandbox (new-sandbox-compiler :tester sandbox-tester 
						 :timeout 10000 
					  	 :object-tester my-obj-tester
                                                 :remember-state 5)))

(def cap 200)

(defn trim [s]
  (let [res (apply str (take cap s))
	rescount (count res)]
    (if (= rescount cap) 
      (str res "... "
           (when (> (count s) cap)
             (try
               (str "http://gist.github.com/" (:repo (new-gist "output.clj" s)))
               (catch java.io.IOException _ nil))))
      res)))

(defmacro defn2 [name & body] `(def ~name (fn ~name ~@body)))

(defn execute-text [txt]
  (try
    (with-open [writer (StringWriter.)]
      (binding [defn #'defn2]
        (let [res (pr-str ((sc txt) {'*out* writer}))
              replaced (.replaceAll (str writer) "\n" " ")]
          (str "\u27F9 " (trim (str replaced (when (= last \space) " ") res))))))
   (catch TimeoutException _ "Execution Timed Out!")
   (catch SecurityException e (str (root-cause e)))
   (catch Exception e (str (root-cause e)))))

(def many (atom 0))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [irc bot channel message]}]
     (.start
      (Thread.
       (fn []
         (when (.startsWith message "->")
           (if (< @many 3)
             (do
               (try
                 (swap! many inc)
                 (send-message irc bot channel (execute-text (apply str (drop 2 message))))
                 (finally (swap! many dec))))
             (send-message irc bot channel "Too much is happening at once. Wait until other operations cease.")))))))))