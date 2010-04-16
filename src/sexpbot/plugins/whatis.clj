(ns sexpbot.plugins.whatis
  (:use [sexpbot respond info gist]
	[clojure.contrib.io :only [spit]])
  (:require [irclj.irclj :as ircb])
  (:import java.io.File))

(def whatis (str (System/getProperty "user.home") "/.sexpbot/whatis.clj"))

(defmethod respond :learn [{:keys [irc channel args]}]
  (let [[subject & is] args
	current (with-info whatis (read-config))]
    (with-info whatis (write-config {subject (apply str (interpose " " is))}))
    (ircb/send-message irc channel "Never shall I forget it.")))

(defmethod respond :whatis [{:keys [irc channel args]}]
  (let [whatmap (with-info whatis (read-config))
	result (-> args first whatmap)]
    (if result
      (ircb/send-message irc channel (str (first args) " = " result))
      (ircb/send-message irc channel (str (first args) " does not exist in my database.")))))

(defmethod respond :forget [{:keys [irc channel args]}]
  (let [whatmap (with-info whatis (read-config))
	subject (first args)]
    (if (whatmap subject) 
      (do (with-info whatis (remove-key subject))
	  (ircb/send-message irc channel (str subject " is removed. RIP.")))
      (ircb/send-message irc channel (str subject " is not in my database.")))))

(defmethod respond :dumpwdb [{:keys [irc channel nick]}]
  (ircb/send-message irc channel 
		     (str nick ": " (with-info whatis 
				      (->> (read-config :string? true) (post-gist "dump.clj"))))))

(defplugin
  {"learn"   :learn
   "whatis"  :whatis
   "forget"  :forget
   "dumpwdb" :dumpwdb})