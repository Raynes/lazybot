(ns sexpbot.respond
  (:use sexpbot.commands))

(defn find-command [cmds command first]
  (let [res (apply merge (remove keyword? (vals cmds)))]
    (cond
     (res first) (res first)
     (cmds command) (cmds command) 
     (some (comp map? val) cmds) (res command))))

(defn cmd-respond [{:keys [command first]}] (find-command @commands command first))

(defmulti respond cmd-respond)

(defmethod respond :quit [{:keys [bot channel]}]
  (.sendMessage bot channel "I bid thee adieu! Into the abyss I go!")
  (System/exit 0))

(defmethod respond :load [{:keys [bot channel args]}]
  (if (modules (-> args first keyword))
    (do 
      (((modules (-> args first keyword)) :load))
      (.sendMessage bot channel "Loaded."))
    (.sendMessage bot channel (str "Module " (first args) " not found."))))

(defmethod respond :unload [{:keys [bot channel args]}]
  (if (modules (-> args first keyword))
    (do 
      (((modules (-> args first keyword)) :unload))
      (.sendMessage bot channel "Unloaded."))
    (.sendMessage bot channel (str "Module " (first args) " not found."))))

(defmethod respond :loaded [{:keys [bot channel args]}]
  (.sendMessage bot channel (str (keys (into {} (filter (comp map? second) @commands))))))

(defmethod respond :default [{:keys [bot channel]}]
  (.sendMessage bot channel "Command not found. You can thank Rayne for this one."))

(defn defmodule [cmd-map m-name]
  (dosync (alter commands merge {m-name cmd-map})
	  (alter modules merge 
		 {m-name 
		  {:load #(dosync (alter commands assoc m-name cmd-map))
		   :unload #(dosync (alter commands dissoc m-name))}})))