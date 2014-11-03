(ns lazybot.plugins.macro
  (:require [lazybot.registry :as registry]
            [lazybot.plugins.login :refer [when-privs]]
            [lazybot.utilities :refer [prefix]]
            [somnium.congomongo :as mongo]))

(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [message bot] :as com-m}]
     (let [macro-body (:macro (mongo/fetch-one :macro :where {:macro-name (.trim message)}))]
       (when (not-empty macro-body)
         (if (some identity (map #(.startsWith macro-body %) (:prepends (:config @bot))))
           (registry/try-handle (assoc com-m :message macro-body))
           (registry/send-message com-m macro-body))))))
			  
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
                     (mongo/destroy! :macro {:macro-name macro-name})
                     (mongo/insert! :macro {:macro-name macro-name :macro macro})
                     (registry/send-message com-m (str "Added macro: " macro-name))))
         (registry/send-message com-m (prefix nick "please provide a macro name and body!"))))))

   (:cmd
    "See what the named macro will do before executing it"
    #{"macroexpand"}
    (fn [{:keys [bot nick args] :as com-m}]
      (let [macro-name (first args)
            macro-body (mongo/fetch-one :macro :where {:macro-name macro-name})]
        (if (seq macro-body)
          (registry/send-message com-m (prefix nick macro-name " => " macro-body))
          (registry/send-message com-m (prefix nick "that macro doesn't exist!"))))))
   (:indexes [[:macro-name]]))
      
    
       
