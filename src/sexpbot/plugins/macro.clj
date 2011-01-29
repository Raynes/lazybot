(ns sexpbot.plugins.macro
  (:use [sexpbot registry info]
        [sexpbot.plugins.login :only [when-privs]]
	clj-config.core
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin :irc
  (:hook
   :on-message
   (fn [{:keys [message bot] :as com-m}]
     (let [macro-body (:macro (fetch-one :macro :where {:macro-name (.trim message)}))]
       (when (not-empty macro-body)
         (if (some identity (map #(.startsWith macro-body %) (:prepends (:config @bot))))
           (try-handle (assoc com-m :message macro-body))
           (send-message com-m macro-body))))))
			  
  (:cmd
   "Add a macro, a shorthand form of writing tedious commands -- Admin only"
   #{"macro"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [macro-name (first args)
           macro      (.trim (->> args (interpose " ") rest (apply str)))]
       (if (and (seq macro)
                (seq macro-name))
         (when-privs com-m :admin
                   (do
                     (destroy! :macro {:macro-name macro-name})
                     (insert! :macro {:macro-name macro-name :macro macro})
                     (send-message com-m (str "Added macro: " macro-name))))
         (send-message com-m (prefix bot nick "please provide a macro name and body!"))))))

   (:cmd
    "See what the named macro will do before executing it"
    #{"macroexpand"}
    (fn [{:keys [bot nick args] :as com-m}]
      (let [macro-name (first args)
            macro-body (fetch-one :macro :where {:macro-name macro-name})]
        (if (seq macro-body)
          (send-message com-m (prefix bot nick macro-name " => " macro-body))
          (send-message com-m (prefix bot nick "that macro doesn't exist!"))))))
   (:indexes [[:macro-name]]))
      
    
       