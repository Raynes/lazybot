(ns sexpbot.respond
  (:use [sexpbot.utilities :only [thunk-timeout]])
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
  (hook-key (apply merge-with concat (map :hooks (vals (:modules @bot))))))

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
       (send-message irc# ~bot (:channel ~irc) (str ~user ": You aren't an admin!")))))

(defn find-command [modules command]
  (some #(when ((:triggers %) command) %) (apply concat (map :commands (vals modules)))))

(defn find-docs [bot command]
  (:doc (find-command (:modules @bot) command)))

(defn respond [{:keys [command bot]}]
  (or (:fn (find-command (:modules @bot) command)) (constantly nil)))

(defn full-prepend [config s]
  ((:prepends config) s))

(defn m-starts-with [m s]
  (some identity (map #(.startsWith m %) s)))

(defn split-args [config s]
  (let [[prepend command & args] (.split s " ")
        is-long-pre (full-prepend config prepend)]
    {:command (if is-long-pre
                command
                (apply str (rest prepend)))
     :args (if is-long-pre args (when command (conj args command)))}))

(def- running (atom 0))

(defn try-handle [{:keys [nick channel irc bot message] :as irc-map}]
  (.start
   (Thread.
    (fn []
      (let [bot-map (assoc irc-map :privs (get-priv (:logged-in @bot) nick))
	    conf (:config @bot)]
	(when (m-starts-with message (:prepends conf))
	  (if (< @running (:max-operations conf))
	    (do
	      (swap! running inc)
	      (try
                (println "intry")
		(thunk-timeout
		 #((respond (into bot-map (split-args conf message))) irc-map)
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
        [hook-fns cmd-fns] ((juxt filter remove) #(= :hook (first %)) (remove #(= :cleanup (first %)) body))
        hook-list (apply merge-with concat (for [[_ hook-this hook-fn] hook-fns] {hook-this [hook-fn]}))
        cmd-list (into [] (for [[_ docs triggers cmd-fn] cmd-fns] {:triggers triggers :doc docs :fn cmd-fn}))]
    `(let [pns# *ns*]
       (defn ~'load-this-plugin [bot#]
         (let [m-name# (keyword (last (.split (str pns#) "\\.")))]
           (dosync
            (alter bot# assoc-in [:modules m-name#]
                   {:commands ~cmd-list
                    :hooks ~hook-list
                    :cleanup ~clean-fn})))))))