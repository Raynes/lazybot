(ns sexpbot.plugins.eval
  (:use net.licenser.sandbox
	clojure.stacktrace
	[net.licenser.sandbox tester matcher]
	sexpbot.respond
	clj-gist.core)
  (:require [irclj.irclj :as ircb])
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(enable-security-manager)

(def sandbox-tester
     (extend-tester secure-tester 
		    (whitelist 
		     (function-matcher '*out* 'println 'print 'pr 'prn 'var 'print-doc 'doc 'throw))))

(def my-obj-tester
     (extend-tester default-obj-tester
		    (whitelist
		     (class-matcher StringWriter String Byte Character StrictMath StringBuffer))))

(def sc (stringify-sandbox (new-sandbox-compiler :tester sandbox-tester 
						 :timeout 10000 
					  	 :object-tester my-obj-tester)))

(def cap 200)

(defn trim [s]
  (let [res (apply str (take cap s))
	rescount (count res)]
    (if (= rescount cap) 
      (str res "... " (when (> (count s) cap) (post-gist "output.clj" s))) 
      res)))

(defn execute-text [txt]
  (try
   (with-open [writer (StringWriter.)]
     (let [res (pr-str ((sc txt) {'*out* writer}))]
       (str "=> " (trim (str (.replaceAll (str writer) "\n" "") " " res)))))
   (catch TimeoutException _ "Execution Timed Out!")
   (catch SecurityException _ "DENIED!")
   (catch Exception e (.getMessage (root-cause e)))))

(defplugin
  (:eval 
   "Evalutate Clojure code. Sandboxed, so you're welcome to try to break it."
   [\( "eval"] 
   [{:keys [irc channel command args]}]
   (ircb/send-message irc channel (->> (if (= (first command) \() 
					 (cons command args) 
					 args)
				       (interpose " ")
				       (apply str) 
				       execute-text))))