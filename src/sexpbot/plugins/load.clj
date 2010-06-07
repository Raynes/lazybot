(ns sexpbot.plugins.load
  (:use [sexpbot respond load]
	[irclj.irclj :as ircb]))

(defplugin
  (:load
   "Load a plugin. ADMIN ONLY!"
   ["load"]
   [{:keys [irc nick channel args] :as irc-map}]
   (if-admin nick irc-map
	     (if (true? (->> args first (loadmod irc)))
	       (ircb/send-message irc channel "Loaded.")
	       (ircb/send-message irc channel (str "Module " (first args) " not found.")))))
  
  (:unload
   "Unload a plugin. ADMIN ONLY!"
   ["unload"]
   [{:keys [irc nick channel args] :as irc-map}]
   (if-admin nick irc-map
	     (if ((:modules @irc) (-> args first keyword))
	       (do 
		 ((((:modules @irc) (-> args first keyword)) :unload))
		 (ircb/send-message irc channel "Unloaded."))
	       (ircb/send-message irc channel (str "Module " (first args) " not found.")))))

  (:loaded
   "Lists all the plugins that are currently loaded. ADMIN ONLY!"
   ["loaded?"]
   [{:keys [irc nick channel args] :as irc-map}]
   (if-admin nick irc-map
	     (ircb/send-message irc channel 
				(->> (:commands @irc) (filter (comp map? second)) (into {}) keys str str))))
  
  (:reload
   "Reloads all plugins. ADMIN ONLY!"
   ["reload"]
   [{:keys [irc channel nick ] :as irc-map}]
   (if-admin nick irc-map (reload-all! irc))))