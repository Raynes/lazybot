(ns sexpbot.respond
  (:use sexpbot.info)
  (:require [irclj.irclj :as ircb]))

(def commands 
     (ref {"load"    :load
	   "unload"  :unload
	   "quit"    :quit
	   "loaded?" :loaded}))

(def logged-in (ref {}))

(def modules (ref {}))

(defn get-priv [user]
  (if (-> user logged-in (= :admin)) :admin :noadmin))

(defmacro if-admin [user & body]
  `(when (= :admin (get-priv ~user)) ~@body))

(defn find-command [cmds command first]
  (let [res (apply merge (remove keyword? (vals cmds)))]
    (cond
     (res first) (res first)
     (cmds command) (cmds command) 
     (some (comp map? val) cmds) (res command))))

(defn cmd-respond [{:keys [command first]} & _] (find-command @commands command first))

(defmulti respond cmd-respond)

(defn split-args [s] (let [[command & args] (.split " " s)]
		       {:command command
			:first (first command)
			:args args}))

(defn loadmod [modu]
  (when (modules (-> modu keyword))
    (((modules (-> modu keyword)) :load)) true))

(defmethod respond :load [{:keys [irc nick channel args]}]
  (if-admin nick 
	    (if (true? (-> args first loadmod))
	      (ircb/send-message irc channel "Loaded.")
	      (ircb/send-message irc channel (str "Module " (first args) " not found.")))))

(defmethod respond :unload [{:keys [irc nick channel args]}]
  (if-admin nick
	    (if (modules (-> args first keyword))
	      (do 
		(((modules (-> args first keyword)) :unload))
		(ircb/send-message irc channel "Unloaded."))
	      (ircb/send-message irc channel (str "Module " (first args) " not found.")))))

(defmethod respond :loaded [{:keys [irc nick channel args]}]
  (if-admin nick
	    (ircb/send-message irc channel 
			  (->> @commands (filter (comp map? second)) (into {}) keys str str))))

(defmethod respond :default [{:keys [irc channel]}]
  (ircb/send-message irc channel "Command not found. No entiendo lo que estás diciendo."))

(defn split-args [s] (let [[command & args] (clojure.contrib.string/split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

(defn handle-message [{:keys [nick message] :as irc-map}]
  (let [bot-map (assoc irc-map :privs (get-priv nick))]
    (if (= (first message) (:prepend (read-config)))
      (-> bot-map (into (->> message rest (apply str) split-args)) respond))))

(defn defplugin [cmd-map]
  (dosync
   (let [m-name (keyword (last (.split (str *ns*) "\\.")))]
     (alter modules merge 
	    {m-name 
	     {:load #(dosync (alter commands assoc m-name cmd-map))
	      :unload #(dosync (alter commands dissoc m-name))}}))))