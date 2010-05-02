(ns sexpbot.respond
  (:use [sexpbot info [utilities :only [thunk-timeout]]])
  (:require [irclj.irclj :as ircb])
  (:import java.util.concurrent.TimeoutException))

(def initial-commands {"load"    {:cmd :load :doc "Loads a module. ADMIN ONLY."}
		       "unload"  {:cmd :unload :doc "Unload's a module. ADMIN ONLY."}
		       "quit"    {:cmd :quit :doc "Makes the bot go bai bai. ADMIN ONLY."}
		       "loaded?" {:cmd :loaded :doc "Returns a list of the currently loaded modules."}
		       "reload"  {:cmd :reload :doc "Reloads respond and all plugins."}
		       "help"    {:cmd :help :doc "Teh help."}})

(def commands 
     (ref initial-commands))

(defn reset-commands [] (dosync (ref-set commands initial-commands)))

(def logged-in (ref {}))
(def modules (ref {}))

(defn reset-ref [aref] (dosync (ref-set aref {})))

(defn get-priv [user]
  (if (-> user logged-in (= :admin)) :admin :noadmin))


(defmacro if-admin [user & body]
  `(if (= :admin (get-priv ~user)) ~@body))

(defn admin? [user]
  (= :admin (get-priv user)))

(defn find-command [cmds command first]
  (let [res (apply merge (remove keyword? (vals cmds)))]
    (cond
     (res first) (res first)
     (cmds command) (cmds command)
     (some (comp map? val) cmds) (res command))))

(defn find-docs [command]
  (:doc (find-command @commands command (first command))))

(defn cmd-respond [{:keys [command first]} & _] (:cmd (find-command @commands command first)))

(defmulti respond cmd-respond)

(defn split-args [s] (let [[command & args] (.split " " s)]
		       {:command command
			:first (first command)
			:args args}))

(defn loadmod [modu]
  (when (modules (-> modu keyword))
    (((modules (-> modu keyword)) :load)) true))

(defn split-args [s] (let [[command & args] (clojure.contrib.string/split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

(defn handle-message [{:keys [nick message] :as irc-map}]
  (let [bot-map (assoc irc-map :privs (get-priv nick))]
    (if (= (first message) (:prepend (read-config)))
      (-> bot-map (into (->> message rest (apply str) split-args)) respond))))

(defn try-handle [{:keys [nick channel irc] :as irc-map}]
  (try
   (thunk-timeout 
    #(try
      (handle-message irc-map)
      (catch Exception e 
	(.printStackTrace e)))
    30)
   (catch TimeoutException _
     (ircb/send-message irc channel "Execution Timed Out!"))))

(def create-initial-hooks
  {:core {:on-message [(fn [irc-map] (try-handle irc-map))]
	  :on-quit []
	  :on-join []}})

(def hooks (ref create-initial-hooks))

(defn reset-hooks [] (dosync (ref-set hooks create-initial-hooks)))

(defn reload-plugins []
  (doseq [plug ((read-config) :plugins)] (require (symbol (str "sexpbot.plugins." plug)) :reload)))

(defn cleanup-plugins []
  (doseq [cfn (map :cleanup (vals @modules))] (cfn)))

(defn reload-all!
  "A clever function to reload everything when running sexpbot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the
  way refs are used. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  []
  (reset-hooks)
  (reset-commands)
  (cleanup-plugins)
  (reset-ref modules)
  (use 'sexpbot.respond :reload)
  (reload-plugins)
  (doseq [plug (:plugins (read-config))] (.start (Thread. (fn [] (loadmod plug))))))

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
       (let [m-name# (keyword (last (.split (str *ns*) "\\.")))]
	 (dosync
	  (alter modules merge 
		 {m-name#
		  {:load #(dosync (alter commands assoc m-name# ~cmd-list)
				  (alter hooks assoc m-name# ~hook-list))
		   :unload #(dosync (alter commands dissoc m-name#)
				    (alter hooks dissoc m-name#))
		   :cleanup ~clean-fn}}))))))


(defmethod respond :load [{:keys [irc nick channel args]}]
  (if (admin? nick) 
    (if (true? (-> args first loadmod))
      (ircb/send-message irc channel "Loaded.")
      (ircb/send-message irc channel (str "Module " (first args) " not found.")))
    (ircb/send-message irc channel (str nick ": you aren't an admin!"))))

(defmethod respond :unload [{:keys [irc nick channel args]}]
  (if (admin? nick)
    (if (modules (-> args first keyword))
      (do 
	(((modules (-> args first keyword)) :unload))
	(ircb/send-message irc channel "Unloaded."))
      (ircb/send-message irc channel (str "Module " (first args) " not found.")))
    (ircb/send-message irc channel (str nick ": you aren't an admin!"))))

(defmethod respond :loaded [{:keys [irc nick channel args]}]
 (if (admin? nick)
   (ircb/send-message irc channel 
		      (->> @commands (filter (comp map? second)) (into {}) keys str str)))
 (ircb/send-message irc channel (str nick ": you aren't an admin!")))

(defmethod respond :reload [{:keys [irc channel nick ]}]
  (if (admin? nick)
    (reload-all!)
    (ircb/send-message irc channel (str nick ": you aren't an admin!"))))

(defmethod respond :help [{:keys [irc nick channel args] :as irc-map}]
  (let [help-msg (.trim 
		  (apply str 
			 (interpose " " 
				    (filter seq 
					    (.split 
					     (apply str (remove #(= \newline %) (find-docs (first args)))) " ")))))]
	(if-not (seq help-msg)
	  (try-handle (assoc irc-map :message (str (:prepend (read-config)) "help- " (->> args
											  (interpose " ")
											  (apply str)))))
	  (ircb/send-message irc channel (str nick ": " help-msg)))))

(defmethod respond :default [{:keys [irc channel]}]
  (ircb/send-message irc channel "Command not found. No entiendo lo que estás diciendo."))