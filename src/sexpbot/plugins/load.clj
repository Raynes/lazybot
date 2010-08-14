(ns sexpbot.plugins.load
  (:use [sexpbot respond load core]))

(defplugin
  (:load
   "Load a plugin. ADMIN ONLY!"
   ["load"]
   [{:keys [irc nick channel args] :as irc-map}]
   (if-admin nick irc-map
	     (if (true? (->> args first (loadmod irc)))
	       (send-message irc channel "Loaded.")
	       (send-message irc channel (str "Module " (first args) " not found.")))))
  
  (:unload
   "Unload a plugin. ADMIN ONLY!"
   ["unload"]
   [{:keys [irc nick channel args] :as irc-map}]
   (if-admin nick irc-map
	     (if ((:modules @irc) (-> args first keyword))
	       (do 
		 ((((:modules @irc) (-> args first keyword)) :unload))
		 (send-message irc channel "Unloaded."))
	       (send-message irc channel (str "Module " (first args) " not found.")))))

  (:loaded
   "Lists all the plugins that are currently loaded. ADMIN ONLY!"
   ["loaded?"]
   [{:keys [irc nick channel args] :as irc-map}]
   (if-admin nick irc-map
	     (send-message irc channel 
				(->> (:commands @irc) (filter (comp map? second)) (into {}) keys str str))))
  
  (:reload
   "Reloads all plugins. ADMIN ONLY!"
   ["reload"]
   [{:keys [irc channel nick ] :as irc-map}]
   (if-admin nick irc-map (apply reload-all! (vals @bots)))))