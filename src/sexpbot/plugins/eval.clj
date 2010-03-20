(ns sexpbot.plugins.eval
  (:use net.licenser.sandbox
	(sexpbot respond commands)))

(def eval-cmds
     {"eval" :eval})

(enable-security-manager)
(def sc (create-sandbox-compiler 'namespace 
				 *default-sandbox-tester* 
				 10000))

(def cap 100)

(defn trim [s]
  (apply str (take cap s)))

(defn execute-text [txt]
  (try
   (trim (str ((sc txt) {})))
   (catch SecurityException _ "DENIED!")
   (catch Exception e (.getMessage (.getCause e)))))

(defmethod respond :eval [{:keys [bot channel command args]}]
  (.sendMessage bot channel (->> args (interpose " ") (apply str) execute-text)))

(defmodule eval-cmds :eval)