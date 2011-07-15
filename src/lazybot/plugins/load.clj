(ns lazybot.plugins.load
  (:use [lazybot registry core irc]
        [lazybot.plugins.login :only [when-privs]]))

(defplugin
  (:cmd
   "Load a plugin. ADMIN ONLY!"
   #{"load"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin
               (if (->> args first (safe-load-plugin com bot))
                 (send-message com-m "Loaded.")
                 (send-message com-m (str "Module " (first args) " not found."))))))
  
  (:cmd
   "Unload a plugin. ADMIN ONLY!"
   #{"unload"}
   (fn [{:keys [bot nick channel args] :as com-m}]
     (when-privs com-m :admin
               (if ((:modules @bot) (keyword (first args)))
                 (do 
                   (dosync (alter bot update-in [:modules] dissoc (keyword (first args))))
                   (send-message com-m "Unloaded."))
                 (send-message com-m (str "Module " (first args) " not found."))))))

  (:cmd
   "Lists all the plugins that are currently loaded. ADMIN ONLY!"
   #{"loaded?"}
   (fn [{:keys [bot nick args] :as com-m}]
     (when-privs com-m :admin
               (send-message com-m
                             (apply str (interpose " " (sort (keys (:modules @bot)))))))))
  
  (:cmd
   "Reloads all plugins. ADMIN ONLY!"
   #{"reload"}
   (fn [{:keys [bot channel nick bot] :as com-m}]
     (when-privs com-m :admin
               (do
                 (apply reload-all (vals @bots))
                 (send-message com-m "Reloaded successfully.")))))

  (:cmd
   "Connect the bot to a server specified in your configuration. ADMIN ONLY!"
   #{"reconnect" "connect"}
   (fn [{:keys [args]}] (connect-bot #'make-bot (first args)))))