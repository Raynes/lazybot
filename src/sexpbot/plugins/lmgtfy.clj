(ns sexpbot.plugins.lmgtfy
  (:use [sexpbot respond])
  )

(defn create-url [args]
  (str "http://www.lmgtfy.com/?q=" (apply str (interpose "+" args))))

(defplugin
  (:lmgtfy 
   "Constructs a lmgtfy URL. If you attach @ nick at the end, it will direct it towards
   the person named by nick."
   ["lmgtfy"]
   [{:keys [irc bot channel args]}]
   (if (not (seq args))
     (send-message irc bot channel "http://www.lmgtfy.com")
     (if (some #(= "@" %) args)
       (let [[url-from user-to] (split-with #(not= "@" %) args)]
	 (send-message irc bot channel (str (last user-to) ": " (create-url url-from))))
       (send-message irc bot channel (create-url args))))))