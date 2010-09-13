(ns sexpbot.plugins.load
  (:use [sexpbot respond core]))

(defplugin
  (:cmd
   "Load a plugin. ADMIN ONLY!"
   #{"load"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot
               (if (true? (->> args first (safe-load-plugin bot)))
                 (send-message irc bot channel "Loaded.")
                 (send-message irc bot channel (str "Module " (first args) " not found."))))))
  
  (:cmd
   "Unload a plugin. ADMIN ONLY!"
   #{"unload"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot
               (if ((:modules @bot) (first args))
                 (do 
                   (dosync (alter bot update-in [:modules] dissoc (first args)))
                   (send-message irc bot channel "Unloaded."))
                 (send-message irc bot channel (str "Module " (first args) " not found."))))))

  (:cmd
   "Lists all the plugins that are currently loaded. ADMIN ONLY!"
   #{"loaded?"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin nick irc-map bot
               (send-message irc bot channel 
                             (apply str (interpose " " (keys (:modules @bot))))))))
  
  (:cmd
   "Reloads all plugins. ADMIN ONLY!"
   #{"reload"}
   (fn [{:keys [irc bot channel nick bot] :as irc-map}]
     (if-admin nick irc-map bot
               (do
                 (apply reload-all (vals @bots))
                 (send-message irc bot channel "Reloaded successfully.")))))

  (:cmd
   "Connect the bot to a server specified in your configuration. ADMIN ONLY!"
   #{"reconnect" "connect"}
   (fn [{:keys [irc bot args]}] (connect-bot (first args))))

  (:cmd
   "Reloads configuration. ADMIN ONLY!"
   #{"reload-config"}
   (fn [{:keys [irc bot nick channel] :as irc-map}]
     (if-admin nick irc-map bot
               (do
                 (dosync (apply reload-config (map (fn [[server {bot :bot}]] [server bot]) @bots)))
                 (send-message irc bot channel "Reloaded successfully."))))))