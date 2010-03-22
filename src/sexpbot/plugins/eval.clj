(ns sexpbot.plugins.eval
  (:use net.licenser.sandbox
	(sexpbot respond commands))
  (:import java.io.StringWriter
	   java.util.concurrent.TimeoutException))

(def eval-cmds
     {"eval" :eval})

(enable-security-manager)

(def sandbox-tester
     (extend-tester secure-tester 
		    (whitelist (function-matcher 'println 'print 'byte 'into-array))
		    (whitelist (class-matcher String Byte Character Double Float Integer Long
					      Math Number Object Short StrictMath StringBuffer))))

(def sc (stringify-sandbox (new-sandbox-compiler :tester sandbox-tester :timeout 10000)))

(def cap 100)

(defn trim [s]
  (let [res (.replaceAll (apply str (take cap s)) "\n" " ")]
    (if (= (count res) 100) (str res "...") res)))

(defn execute-text [txt]
  (let [writer (StringWriter.)]
    (try
     (trim (str writer ((sc txt) {'*out* writer})))
     (catch TimeoutException _ "Execution Timed Out!")
     (catch SecurityException _ "DENIED!")
     (catch Exception e (.getMessage (.getCause e))))))

(defmethod respond :eval [{:keys [bot channel command args]}]
  (.sendMessage bot channel (->> args (interpose " ") (apply str) execute-text)))

(defmodule eval-cmds :eval)