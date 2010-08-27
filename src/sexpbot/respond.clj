(ns sexpbot.respond
  (:use [sexpbot info [utilities :only [thunk-timeout]]]
	[clj-config.core :only [read-config get-key]])
  (:require [irclj.irclj :as ircb])
  (:import java.util.concurrent.TimeoutException))

(defmacro def- [name & value]
  (concat (list 'def (with-meta name (assoc (meta name) :private true))) value))

;; I'm sleepy, so I'm using loop. Don't judge me. Fuck off.
(defn nil-comp [irc bot channel s & fns]
  (loop [cs s f fns]
    (if (seq f)
      (when-let [new-s ((first f) @irc @bot channel cs)]
        (recur new-s (rest fns)))
      cs)))

(defn pull-hooks [bot hook-key]
  (hook-key (apply merge-with concat (vals (:hooks @bot)))))

(defn call-message-hooks [irc bot channel s]
  (apply nil-comp irc bot channel s (pull-hooks bot :on-send-message)))

(defn send-message [irc bot channel s]
  (if-let [result (call-message-hooks irc bot channel s)]
    (do (ircb/send-message irc channel result)
        :success)
    :failure))

(defn get-priv [logged-in user]
  (if (and (seq logged-in) (-> user logged-in (= :admin))) :admin :noadmin))

(defmacro if-admin
  [user irc bot & body]
  `(let [irc# (:irc ~irc)]
     (if (and (seq (:logged-in @~bot))
              (= :admin (get-priv ((:logged-in @~bot) (:server @irc#)) ~user)))
       ~@body
       (send-message irc# (:channel ~irc) (str ~user ": You aren't an admin!")))))

(defn find-command [cmds command first]
  (let [res (apply merge (remove keyword? (vals cmds)))]
    (cond
     (res first) (res first)
     (cmds command) (cmds command)
     (some (comp map? val) cmds) (res command))))

(defn find-docs [bot command]
  (:doc (find-command (:commands @bot) command (first command))))

(defn cmd-respond [{:keys [command first bot]} & _] (:cmd (find-command (:commands @bot) command first)))

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

(def- running (atom 0))

(defn try-handle [{:keys [nick channel irc bot message] :as irc-map}]
  (.start
   (Thread.
    (fn []
      (let [bot-map (assoc irc-map :privs (get-priv (:logged-in @bot) nick))
	    conf (read-config info-file)]
	(when (m-starts-with message (:prepends conf))
	  (if (< @running (:max-operations conf))
	    (do
	      (swap! running inc)
	      (try
		(thunk-timeout
		 #(-> bot-map (into (split-args message)) respond)
		 30)
		(catch TimeoutException _ (send-message irc channel "Execution timed out."))
		(catch Exception e (.printStackTrace e))
		(finally (swap! running dec))))
	    (send-message irc channel "Too much is happening at once. Wait until other operations cease."))))))))

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
	 (defn ~'load-this-plugin [bot#]
	   (let [m-name# (keyword (last (.split (str pns#) "\\.")))]
	     (dosync
	      (alter bot# assoc-in [:modules m-name#]
		     {:load (fn [] (dosync (alter bot# assoc-in [:commands m-name#] ~cmd-list)
					   (alter bot# assoc-in [:hooks m-name#] ~hook-list)
                                           (alter bot# assoc-in [:configs m-name#] {})))
		      :unload (fn [] (dosync (alter bot# update-in [:commands] dissoc m-name#)
					     (alter bot# update-in [:hooks] dissoc m-name#)
                                             (alter bot# update-in [:configs] dissoc m-name#)))
		      :cleanup ~clean-fn}))))))))

; Disabled for now. Will make this a configuration option in a little while.
(defmethod respond :default [{:keys [irc channel]}]
           #_(send-message irc channel "Command not found. No entiendo lo que estás diciendo."))