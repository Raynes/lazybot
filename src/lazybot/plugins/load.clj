(ns lazybot.plugins.load
  (:require [lazybot.registry :as registry]
            [lazybot.core :as lazybot]
            [lazybot.irc :as irc]
            [lazybot.plugins.login :refer [when-privs]]))

(registry/defplugin
  (:cmd
   "Load a plugin. ADMIN ONLY!"
   #{"load"}
   (fn [{:keys [com bot user-nick channel args] :as com-m}]
     (when-privs com-m :admin
               (if (->> args first (lazybot/safe-load-plugin com bot))
                 (registry/send-message com-m "Loaded.")
                 (registry/send-message com-m (str "Module " (first args) " not found."))))))
  
  (:cmd
   "Unload a plugin. ADMIN ONLY!"
   #{"unload"}
   (fn [{:keys [bot user-nick channel args] :as com-m}]
     (when-privs com-m :admin
               (if ((:modules @bot) (keyword (first args)))
                 (do 
                   (dosync (alter bot update-in [:modules] dissoc (keyword (first args))))
                   (registry/send-message com-m "Unloaded."))
                 (registry/send-message com-m (str "Module " (first args) " not found."))))))

  (:cmd
   "Lists all the plugins that are currently loaded. ADMIN ONLY!"
   #{"loaded"}
   (fn [{:keys [bot user-nick args] :as com-m}]
     (when-privs com-m :admin
                 (registry/send-message com-m
                             (apply str (interpose " " (sort (keys (:modules @bot)))))))))
  
  (:cmd
   "Reloads all plugins. ADMIN ONLY!"
   #{"reload"}
   (fn [{:keys [bot channel user-nick bot] :as com-m}]
     (when-privs com-m :admin
               (do
                 (apply lazybot/reload-all (vals @lazybot/bots))
                 (registry/send-message com-m "Reloaded successfully.")))))

  (:cmd
   "Connect the bot to a server specified in your configuration. ADMIN ONLY!"
   #{"reconnect" "connect"}
   (fn [{:keys [args]}] (irc/init-bot (first args)))))
