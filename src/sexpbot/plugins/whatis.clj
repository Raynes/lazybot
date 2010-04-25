(ns sexpbot.plugins.whatis
  (:use [sexpbot respond info gist]
	[clojure.contrib.io :only [spit]]
	stupiddb.core)
  (:require [irclj.irclj :as ircb])
  (:import java.io.File))

(def whatis (str (System/getProperty "user.home") "/.sexpbot/whatis.db"))

(def db (db-init whatis 30))

(defmethod respond :learn [{:keys [irc channel args]}]
  (let [[subject & is] args
	is-s (apply str (interpose " " is))]
    (do
      (db-assoc db subject is-s)
      (ircb/send-message irc channel "My memory is more powerful than M-x butterfly. I wont forget it."))))

(defmethod respond :whatis [{:keys [irc channel args]}]
  (let [result (->> args first (db-get db))]
    (if result
      (ircb/send-message irc channel (str (first args) " = " result))
      (ircb/send-message irc channel (str (first args) " does not exist in my database.")))))

(defmethod respond :forget [{:keys [irc channel args]}]
  (let [subject (first args)]
    (if (db-get db subject) 
      (do (db-dissoc db subject)
	  (ircb/send-message irc channel (str subject " is removed. RIP.")))
      (ircb/send-message irc channel (str subject " is not in my database.")))))

(defmethod respond :rwhatis [{:keys [irc channel]}]
  (let [whatmap (with-info whatis (read-config))
	key (nth (keys whatmap) (rand-int (count whatmap)))]
    (ircb/send-message irc channel
		       (str key " = " (whatmap key)))))

(defmethod respond :dumpwdb [{:keys [irc channel nick]}]
  (ircb/send-message irc channel 
		     (str nick ": " (with-info whatis 
				      (->> (read-config :string? true) (post-gist "dump.clj"))))))

(defplugin
  {"learn"   :learn
   "rwhatis" :rwhatis
   "whatis"  :whatis
   "forget"  :forget
   "dumpwdb" :dumpwdb})