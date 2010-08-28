(ns sexpbot.plugins.macro
  (:use [sexpbot respond info]
	clj-config.core
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin
  (:add-hook :on-message
	     (fn [{:keys [irc bot nick message channel] :as irc-map}]
	       (let [macro-body (:macro (fetch-one :macro :where {:macro-name (.trim message)}))]
		 (when (not-empty macro-body)
		   (if (some identity (map #(.startsWith macro-body %) (:prepends (read-config info-file))))
		     (try-handle (assoc irc-map :message macro-body))
		     (send-message irc bot channel macro-body))))))
			  
  (:macro
   "Add a macro, a shorthand form of writing tedious commands -- Admin only"
   ["macro"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (let [macro-name (first args)
	 macro      (.trim (->> args (interpose " ") rest (apply str)))]
     (if (and (seq macro)
	      (seq macro-name))
       (if-admin nick irc-map bot
                 (do
                   (destroy! :macro {:macro-name macro-name})
                   (insert! :macro {:macro-name macro-name :macro macro})
                   (send-message irc bot channel (str "Added macro: " macro-name))))
       (send-message irc bot channel (str nick ": please provide a macro name and body!")))))

   (:macroexpand
    "See what the named macro will do before executing it"
    ["macroexpand"]
    [{:keys [irc bot nick channel args] :as irc-map}]
    (let [macro-name (first args)
	  macro-body (fetch-one :macro :where {:macro-name macro-name})]
      (if (seq macro-body)
	(send-message irc bot channel (str nick ": " macro-name " => " macro-body))
	(send-message irc bot channel (str nick ": that macro doesn't exist!"))))))
      
    
       