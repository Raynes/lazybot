(ns sexpbot.plugins.load
  (:use [sexpbot respond load core]))

(defplugin
  (:load
   "Load a plugin. ADMIN ONLY!"
   ["load"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot
	     (if (true? (->> args first (loadmod irc)))
	       (send-message irc bot channel "Loaded.")
	       (send-message irc bot channel (str "Module " (first args) " not found.")))))
  
  (:unload
   "Unload a plugin. ADMIN ONLY!"
   ["unload"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot
	     (if ((:modules @bot) (-> args first keyword))
	       (do 
		 ((((:modules @bot) (-> args first keyword)) :unload))
		 (send-message irc bot channel "Unloaded."))
	       (send-message irc bot channel (str "Module " (first args) " not found.")))))

  (:loaded
   "Lists all the plugins that are currently loaded. ADMIN ONLY!"
   ["loaded?"]
   [{:keys [irc bot nick channel args] :as irc-map}]
   (if-admin nick irc-map bot
	     (send-message irc bot channel 
                           (->> (:commands @bot) (filter (comp map? second)) (into {}) keys str str))))
  
  (:reload
   "Reloads all plugins. ADMIN ONLY!"
   ["reload"]
   [{:keys [irc channel nick bot] :as irc-map}]
   (if-admin nick irc-map bot (apply reload-all! (vals @bots)))))