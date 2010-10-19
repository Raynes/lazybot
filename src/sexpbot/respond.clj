(ns sexpbot.respond
  (:use [sexpbot.utilities :only [thunk-timeout]])
  (:require [irclj.irclj :as ircb])
  (:import java.util.concurrent.TimeoutException))

(defmacro def- [name & value]
  (concat (list 'def (with-meta name (assoc (meta name) :private true))) value))

;; I'm sleepy, so I'm using loop. Don't judge me. Fuck off.
(defn nil-comp [irc bot channel s action? & fns]
  (loop [cs s f fns]
    (if (seq f)
      (when-let [new-s ((first f) irc bot channel cs action?)]
        (recur new-s (next f)))
      cs)))

(defn pull-hooks [bot hook-key]
  (hook-key (apply merge-with concat (map :hooks (vals (:modules @bot))))))

(defn call-message-hooks [irc bot channel s action?]
  (apply nil-comp irc bot channel s action? (pull-hooks bot :on-send-message)))

(defn send-message [irc bot channel s & {action? :action?}]
  (if-let [result (call-message-hooks irc bot channel s action?)]
    (do (if action?
          (ircb/send-action irc channel result)
          (ircb/send-message irc channel result))
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
  (:docs (find-command (:modules @bot) command)))

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
                (let [n-bmap (into bot-map (split-args conf message))]
                  (thunk-timeout #((respond n-bmap) n-bmap) 30))
		(catch TimeoutException _ (send-message irc bot channel "Execution timed out."))
		(catch Exception e (.printStackTrace e))
		(finally (swap! running dec))))
	    (send-message irc bot channel "Too much is happening at once. Wait until other operations cease."))))))))

(defn merge-with-conj [& args]
  (apply merge-with #(if (vector? %) (conj % %2) (conj [] % %2)) args))

(defn parse-fns [body]
  (apply merge-with-conj
         (for [[one two three four] body]
           (case
            one
            :cmd {:cmd {:docs two :triggers three :fn four}}
            :hook {:hook {two three}}
            :cleanup {:cleanup two}
            :init {:init two}
            :routes {:routes two}))))

(defn if-seq-error [fn-type possible-seq]
  (if (and (not (fn? possible-seq)) (seq possible-seq))
    (throw (Exception. (str "Only one " fn-type " function allowed.")))
    possible-seq))

(defmacro defplugin [& body]
  (let [{:keys [cmd hook cleanup init routes]} (parse-fns body)
        scmd (if (map? cmd) [cmd] cmd)]
    `(let [pns# *ns*]
       (defn ~'load-this-plugin [irc# bot#]
         (when ~init ((if-seq-error "init" ~init) irc# bot#))
         (let [m-name# (keyword (last (.split (str pns#) "\\.")))]
           (dosync
            (alter bot# assoc-in [:modules m-name#]
                   {:commands ~scmd
                    :hooks (into {}
                                 (map (fn [[k# v#]] (if (vector? v#) [k# v#] [k# [v#]]))
                                      (apply merge-with-conj (if (vector? ~hook) ~hook [~hook]))))
                    :cleanup (if-seq-error "cleanup" ~cleanup)
                    :routes ~routes})))))))