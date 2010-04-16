(ns sexpbot.plugins.whatis
  (:use [sexpbot respond info gist]
	[clojure.contrib.duck-streams :only [spit]])
  (:require [irclj.irclj :as ircb])
  (:import java.io.File))

(def whatis (str (System/getProperty "user.home") "/.sexpbot/whatis.clj"))

(defmethod respond :learn [{:keys [bot channel args]}]
  (let [[subject & is] args
	current (with-info whatis (read-config))]
    (with-info whatis (write-config {subject (apply str (interpose " " is))}))
    (ircb/send-message bot channel "Never shall I forget it.")))

(defmethod respond :whatis [{:keys [bot channel args]}]
  (let [whatmap (with-info whatis (read-config))
	result (-> args first whatmap)]
    (if result
      (ircb/send-message bot channel (str (first args) " = " result))
      (ircb/send-message bot channel (str (first args) " does not exist in my database.")))))

(defmethod respond :forget [{:keys [bot channel args]}]
  (let [whatmap (with-info whatis (read-config))
	subject (first args)]
    (if (whatmap subject) 
      (do (with-info whatis (remove-key subject))
	  (ircb/send-message bot channel (str subject " is removed. RIP.")))
      (ircb/send-message bot channel (str subject " is not in my database.")))))

(defmethod respond :dumpwdb [{:keys [bot channel sender]}]
  (ircb/send-message bot channel 
		     (str sender ": " (->> (read-config) 
					   (with-info whatis) 
					   format-config
					   (post-gist "dump.clj")))))

(defplugin
  {"learn"   :learn
   "whatis"  :whatis
   "forget"  :forget
   "dumpwdb" :dumpwdb})