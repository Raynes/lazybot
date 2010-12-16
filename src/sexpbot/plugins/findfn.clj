(ns sexpbot.plugins.findfn
  (:use [sexpbot registry])
  (:require [sexpbot.plugins.clojure :as sandbox]
	    [clojure.string :as s]))

(defn find-fn
  [in out]
  (map first (filter
	       (fn [x]
		 (try 
		   (= out
		      (binding [*out* java.io.StringWriter]
			(apply
			 (if (-> (second x) meta :macro)
			   (macroexpand `(second x))
			   (second x))
			 in)))
		   (catch Exception _ false)))
	       (ns-publics (the-ns `clojure.core)))))

(defplugin
  (:cmd
   "Finds the clojure fns, which given your input, produce your ouput."
   #{"findfn"}
   (fn [{:keys [bot args] :as com-m}]
     (let [[user-in user-out :as args] (with-in-str (s/join " " args) [(read) (read)])]
       (send-message com-m (str (vec (sandbox/sb `(find-fn ~user-in ~user-out)))))))))