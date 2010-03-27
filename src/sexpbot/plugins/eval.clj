(ns sexpbot.plugins.eval
  (:use net.licenser.sandbox
	clojure.stacktrace
	(net.licenser.sandbox tester matcher)
	(sexpbot respond commands))
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(enable-security-manager)

(def sandbox-tester
     (extend-tester secure-tester 
		    (whitelist 
		     (function-matcher 'println 'print 'doc 'char 'apply 'print-doc))
		    (whitelist 
		     (class-matcher String Byte Character Double Float Integer Long
				    Math Number Short StrictMath StringBuffer))))

(def sc (stringify-sandbox (new-sandbox-compiler :tester sandbox-tester :timeout 10000)))

(def cap 300)

(defn trim [s]
  (let [res (.replaceAll (apply str (take cap s)) "\n" " ")]
    (if (= (count res) 100) (str res "...") res)))

(defn execute-text [txt]
  (let [writer (StringWriter.)]
    (try
     (trim (str writer ((sc txt) {'*out* writer})))
     (catch TimeoutException _ "Execution Timed Out!")
     (catch SecurityException _ "DENIED!")
     (catch Exception e (.getMessage (root-cause e))))))

(defmethod respond :eval [{:keys [bot channel command args]}]
  (.sendMessage bot channel (->> (cons command args) 
				 (interpose " ") 
				 (apply str) 
				 execute-text)))

;;(defmethod respond :doc [{:keys [bot channel args]}]
  ;;(.sendMessage bot channel (my-doc (first args))))

(defmodule :eval
  {\(     :eval
   "eval" :eval})