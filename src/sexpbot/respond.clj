(ns sexpbot.respond
  (:use [sexpbot info [utilities :only [thunk-timeout]]]
	[clj-config.core :only [read-config get-key]])
  (:require [irclj.irclj :as ircb])
  (:import java.util.concurrent.TimeoutException))

(defn get-priv [logged-in user]
  (if (and (seq logged-in) (-> user logged-in (= :admin))) :admin :noadmin))

(defmacro if-admin
  [user irc & body]
  `(let [irc# (:irc ~irc)]
     (cond
      (and (seq (:logged-in @irc#)) (= :admin (get-priv ((:logged-in @irc#) (:server @irc#)) ~user))) ~@body
      :else (ircb/send-message irc# (:channel ~irc) (str ~user ": You aren't an admin!")))))

(defn find-command [cmds command first]
  (let [res (apply merge (remove keyword? (vals cmds)))]
    (cond
     (res first) (res first)
     (cmds command) (cmds command)
     (some (comp map? val) cmds) (res command))))

(defn find-docs [irc command]
  (:doc (find-command (:commands @irc) command (first command))))

(defn cmd-respond [{:keys [command first irc]} & _] (:cmd (find-command (:commands @irc) command first)))

(defmulti respond cmd-respond)

(defn full-prepend [s]
  ((get-key :prepends info-file) s))

(defn m-starts-with [m s]
  (some identity (map #(.startsWith m %) s)))

(defn split-args [s]
  (let [[prepend command & args] (.split s " ")
        is-long-pre (full-prepend prepend)]
    {:command (if is-long-pre
                command
                (apply str (rest prepend)))
     :first (if is-long-pre (first command) (first (rest prepend)))
     :args (if is-long-pre args (when command (conj args command)))}))

(def running (atom 0))

(defn try-handle [{:keys [nick channel irc message] :as irc-map}]
  (.start
   (Thread.
    (fn []
      (let [bot-map (assoc irc-map :privs (get-priv (:logged-in @irc) nick))
	    conf (read-config info-file)]
	(when (m-starts-with message (:prepends conf))
	  (if (< @running (:max-operations conf))
	    (do
	      (swap! running inc)
	      (try
		(thunk-timeout
		 #(-> bot-map (into (split-args message)) respond)
		 30)
		(catch TimeoutException _ (ircb/send-message irc channel "Execution timed out."))
		(catch Exception e (.printStackTrace e))
		(finally (swap! running dec))))
	    (ircb/send-message irc channel "Too much is happening at once. Wait until other operations cease."))))))))

;; Thanks to mmarczyk, Chousuke, and most of all cgrand for the help writing this macro.
;; It's nice to know that you have people like them around when it comes time to face
;; unfamiliar concepts.
(defmacro defplugin [& body]
  (let [clean-fn (if-let [cfn (seq (filter #(= :cleanup (first %)) body))] (second (first cfn)) `(fn [] nil))
	[hook-fns methods] ((juxt filter remove) #(= :add-hook (first %)) (remove #(= :cleanup (first %)) body))
	cmd-list (into {} (for [[cmdkey docs words] methods word words] [word {:cmd cmdkey :doc docs}]))
	hook-list (apply merge-with concat (for [[_ hook-this hook-fn] hook-fns] {hook-this [hook-fn]}))]
    `(do
       ~@(for [[cmdkey docs words & method-stuff] methods]
	   `(defmethod respond ~cmdkey ~@method-stuff))
       (let [pns# *ns*]
	 (defn ~'load-this-plugin [irc#]
	   (let [m-name# (keyword (last (.split (str pns#) "\\.")))]
	     (dosync
	      (alter irc# assoc-in [:modules m-name#]
		     {:load (fn [] (dosync (alter irc# assoc-in [:commands m-name#] ~cmd-list)
					   (alter irc# assoc-in [:hooks m-name#] ~hook-list)))
		      :unload (fn [] (dosync (alter irc# update-in [:commands] dissoc m-name#)
					     (alter irc# update-in [:hooks] dissoc m-name#)))
		      :cleanup ~clean-fn}))))))))

(defmethod respond :default [{:keys [irc channel]}]
  (ircb/send-message irc channel "Command not found. No entiendo lo que estás diciendo."))