(ns sexpbot.commands)

(def commands 
     (ref {"load" :load
	   "unload" :unload
	   "quit" :quit
	   "loaded?" :loaded}))

(def logged-in (ref {}))

(def modules (ref {}))