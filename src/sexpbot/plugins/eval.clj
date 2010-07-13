(ns sexpbot.plugins.eval
  (:use net.licenser.sandbox
	clojure.stacktrace
	[net.licenser.sandbox tester matcher]
	sexpbot.respond
	[clj-github.gists :only [new-gist]])
  (:require [irclj.irclj :as ircb])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(enable-security-manager)

(def sandbox-tester
     (extend-tester secure-tester 
		    (whitelist 
		     (function-matcher '*out* 'println 'print 'pr 'prn 'var 'print-doc 'doc 'throw
                                       'def 'def*)
		     (class-matcher java.io.StringWriter java.net.URL java.net.URI))))

(def my-obj-tester
     (extend-tester default-obj-tester
		    (whitelist
		     (class-matcher java.io.StringWriter String Byte Character StrictMath StringBuffer
				    java.net.URL java.net.URI))))

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

(defn execute-text [txt]
  (try
   (with-open [writer (StringWriter.)]
     (let [res (pr-str ((sc txt) {'*out* writer}))]
       (str "=> " (trim (str (.replaceAll (str writer) "\n" " ") " " res)))))
   (catch TimeoutException _ "Execution Timed Out!")
   (catch SecurityException e (str (root-cause e)))
   (catch Exception e (str (root-cause e)))))

(defplugin
  (:add-hook :on-message
             (fn [{:keys [irc channel message]}]
               (when (.startsWith message "=>")
                 (ircb/send-message irc channel (execute-text (apply str (drop 2 message)))))))
  (:eval
   "Old. Use => now. It's a hook, so it can evaluate anything."
   [\( "eval"]
   [{:keys [irc channel]}]
   (ircb/send-message irc channel "This command is old. Use => now. It's a hook, so it can evaluate anything, even stuff that doesn't start with parentheses.")))