(ns sexpbot.plugins.whatis
  (:use [sexpbot respond info utilities]
	clj-gist.core
	[clojure.contrib.io :only [spit]]
	clj-config.core
	stupiddb.core)
  (:require [irclj.irclj :as ircb])
  (:import java.io.File))

(def whatis (str (System/getProperty "user.home") "/.sexpbot/whatis.db"))

(def db (db-init whatis 1800))

(defplugin 
  (:learn 
   "Teaches the bot a new thing. It takes a name and whatever you want to assign the name
   to. For example: $learn me a human being."
   ["learn"] 
   [{:keys [irc channel args]}]
   (let [[subject & is] args
	 is-s (apply str (interpose " " is))]
     (do
       (db-assoc db subject is-s)
       (ircb/send-message irc channel "My memory is more powerful than M-x butterfly. I wont forget it."))))
   
   (:whatis 
    "Pass it a key, and it will tell you what is at the key in the database."
    ["whatis"]
    [{:keys [irc channel args]}]
    (let [result (->> args first (db-get db))]
      (if result
	(ircb/send-message irc channel (str (first args) " = " result))
	(ircb/send-message irc channel (str (first args) " does not exist in my database.")))))
   
   (:forget 
    "Forgets the value of a key."
    ["forget"] 
    [{:keys [irc channel args]}]
    (let [subject (first args)]
      (if (db-get db subject) 
	(do (db-dissoc db subject)
	    (ircb/send-message irc channel (str subject " is removed. RIP.")))
	(ircb/send-message irc channel (str subject " is not in my database.")))))

   (:rwhatis 
    "Gets a random value from the database."
    ["rwhatis"] 
    [{:keys [irc channel]}]
    (let [whatmap (read-config whatis)
	  key (nth (keys whatmap) (rand-int (count whatmap)))]
      (ircb/send-message irc channel
			 (str key " = " (whatmap key)))))
   
   (:dumpwdb 
    "Dumps the entire database to a gist."
    ["dumpwdb"]
    [{:keys [irc channel nick]}]
    (ircb/send-message irc channel 
		       (str nick ": " (->> (read-config whatis :string? true) (post-gist "dump.clj")))))
   (:cleanup (fn [] (db-close db))))