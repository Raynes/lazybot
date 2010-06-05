(ns sexpbot.respond
  (:use [sexpbot info [utilities :only [thunk-timeout]]]
	[clj-config.core :only [read-config]])
  (:require [irclj.irclj :as ircb])
  (:import java.util.concurrent.TimeoutException))

(def initial-commands {"load"    {:cmd :load :doc "Loads a module. ADMIN ONLY."}
		       "unload"  {:cmd :unload :doc "Unload's a module. ADMIN ONLY."}
		       "quit"    {:cmd :quit :doc "Makes the bot go bai bai. ADMIN ONLY."}
		       "loaded?" {:cmd :loaded :doc "Returns a list of the currently loaded modules."}
		       "reload"  {:cmd :reload :doc "Reloads respond and all plugins."}
		       "help"    {:cmd :help :doc "Teh help."}})

;(defn reset-commands [] (dosync (ref-set commands initial-commands)))

(defn reset-ref [aref] (dosync (ref-set aref {})))

(defn get-priv [logged-in user]
  (if (and (seq logged-in) (-> user logged-in (= :noadmin))) :admin :noadmin))

(defmacro if-admin
  [user irc & body]
  `(cond
    (= :admin (get-priv (:logged-in @~irc) ~user)) ~@body
    :else (ircb/send-message (:irc ~irc) (:channel ~irc) (str ~user ": You aren't an admin!"))))

(defn find-command [cmds command first]
  (println "looking up command.")
  (let [res (apply merge (remove keyword? (vals cmds)))]
    (cond
     (res first) (res first)
     (cmds command) (cmds command)
     (some (comp map? val) cmds) (res command))))

(defn find-docs [irc command]
  (:doc (find-command (:commands @irc) command (first command))))

(defn cmd-respond [{:keys [command first irc]} & _] (:cmd (find-command (:commands @irc) command first)))

(defmulti respond cmd-respond)

(defn split-args [s] (let [[command & args] (.split " " s)]
		       {:command command
			:first (first command)
			:args args}))

(defn loadmod [irc modu]
  (when ((:modules @irc) (-> modu keyword))
    ((((:modules @irc) (-> modu keyword)) :load)) true))

(defn split-args [s] (let [[command & args] (clojure.contrib.string/split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

(def running (ref 0))

(defn try-handle [{:keys [nick channel irc message] :as irc-map}]
  (.start
   (Thread.
    (fn []
      (let [bot-map (assoc irc-map :privs (get-priv (:logged-in @irc) nick))
	    conf (read-config info-file)]
	(when (= (first message) (:prepend conf))
	  (if (< @running (:max-operations conf))
	    (do
	      (dosync (alter running inc))
	      (try
		(thunk-timeout
		 #(-> bot-map (into (->> message rest (apply str) split-args)) respond)
		 30)
		(catch TimeoutException _ (ircb/send-message irc channel "Execution timed out."))
		(catch Exception e (.printStackTrace e))
		(finally (dosync (alter running dec)))))
	    (ircb/send-message irc channel "Too much is happening at once. Wait until other operations cease."))))))))
  
(def create-initial-hooks
  {:core {:on-message [(fn [irc-map] (try-handle irc-map))]
	  :on-quit []
	  :on-join []}})

;(def hooks (ref create-initial-hooks))

;(defn reset-hooks [] (dosync (ref-set hooks create-initial-hooks)))

(defn require-plugins []
  (doseq [plug ((read-config info-file) :plugins)]
    (let [prefix (str "sexpbot.plugins." plug)]
      (require (symbol prefix) :reload))))

(defn load-plugins [irc]
  (doseq [plug ((read-config info-file) :plugins)]
    (let [prefix (str "sexpbot.plugins." plug)]
      ((resolve (symbol (str prefix "/load-this-plugin"))) irc))))

;(defn cleanup-plugins []
;  (doseq [cfn (map :cleanup (vals @modules))] (cfn)))

(defn reload-all!
  "A clever function to reload everything when running sexpbot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the
  way refs are used. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  [irc]
 ; (reset-hooks)
 ; (reset-commands)
 ; (cleanup-plugins)
 ; (reset-ref modules)
  (use 'sexpbot.respond :reload)
  (require-plugins)
  (load-plugins irc)
  (doseq [plug (:plugins (read-config info-file))] (.start (Thread. (fn [] (loadmod plug))))))

;; Thanks to mmarczyk, Chousuke, and most of all cgrand for the help writing this macro.
;; It's nice to know that you have people like them around when it comes time to face
;; unfamiliar concepts.
(defmacro defplugin [& body]
  (let [clean-fn (if-let [cfn (seq (filter #(= :cleanup (first %)) body))] (second (first cfn)) (fn []))
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
		     {:load #(dosync (alter irc# assoc-in [:commands m-name#] ~cmd-list)
				     (alter irc# assoc-in [:hooks m-name#] ~hook-list))
		      :unload #(dosync (alter irc# update-in [:commands] dissoc m-name#)
				       (alter irc# update-in [:hooks] dissoc m-name#))
		      :cleanup ~clean-fn}))))))))


(defmethod respond :load [{:keys [irc nick channel args] :as irc-map}]
  (if-admin nick irc-map
    (if (true? (->> args first (loadmod irc)))
      (ircb/send-message irc channel "Loaded.")
      (ircb/send-message irc channel (str "Module " (first args) " not found.")))))

(defmethod respond :unload [{:keys [irc nick channel args] :as irc-map}]
  (if-admin nick irc-map
    (if ((:modules @irc) (-> args first keyword))
      (do 
	((((:modules @irc) (-> args first keyword)) :unload))
	(ircb/send-message irc channel "Unloaded."))
      (ircb/send-message irc channel (str "Module " (first args) " not found.")))))

(defmethod respond :loaded [{:keys [irc nick channel args] :as irc-map}]
 (if-admin nick irc-map
   (ircb/send-message irc channel 
		      (->> (:commands @irc) (filter (comp map? second)) (into {}) keys str str))))

(defmethod respond :reload [{:keys [irc channel nick ] :as irc-map}]
  (if-admin nick irc-map (reload-all!)))

(defmethod respond :help [{:keys [irc nick channel args] :as irc-map}]
  (let [help-msg (.trim 
		  (apply str 
			 (interpose " " 
				    (filter seq 
					    (.split 
					     (apply str (remove #(= \newline %) (find-docs irc (first args)))) " ")))))]
    (if-not (seq help-msg)
      (try-handle (assoc irc-map :message (str (:prepend (read-config info-file)) 
					       "help- " (->> args (interpose " ") (apply str)))))
      (ircb/send-message irc channel (str nick ": " help-msg)))))

(defmethod respond :default [{:keys [irc channel]}]
  (ircb/send-message irc channel "Command not found. No entiendo lo que estás diciendo."))