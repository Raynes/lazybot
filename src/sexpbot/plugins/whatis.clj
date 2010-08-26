(ns sexpbot.plugins.whatis
  (:use [sexpbot respond info utilities]
	clj-config.core
	stupiddb.core)
  
  (:import java.io.File))

(def whatis (str (System/getProperty "user.home") "/.sexpbot/whatis.db"))

(def db (db-init whatis 10))

(defplugin 
  (:learn 
   "Teaches the bot a new thing. It takes a name and whatever you want to assign the name
   to. For example: $learn me a human being."
   ["learn"] 
   [{:keys [irc bot channel args]}]
   (let [[subject & is] args
	 is-s (apply str (interpose " " is))]
     (do
       (db-assoc db subject is-s)
       (send-message irc bot channel "My memory is more powerful than M-x butterfly. I wont forget it."))))
   
   (:whatis 
    "Pass it a key, and it will tell you what is at the key in the database."
    ["whatis"]
    [{:keys [irc bot channel args]}]
    (let [result (->> args first (db-get db))]
      (if result
	(send-message irc bot channel (str (first args) " = " result))
	(send-message irc bot channel (str (first args) " does not exist in my database.")))))
   
   (:forget 
    "Forgets the value of a key."
    ["forget"] 
    [{:keys [irc bot channel args]}]
    (let [subject (first args)]
      (if (db-get db subject) 
	(do (db-dissoc db subject)
	    (send-message irc bot channel (str subject " is removed. RIP.")))
	(send-message irc bot channel (str subject " is not in my database.")))))

   (:rwhatis 
    "Gets a random value from the database."
    ["rwhatis"] 
    [{:keys [irc bot channel]}]
    (let [whatmap (read-config whatis)
	  key (nth (keys whatmap) (rand-int (count whatmap)))]
      (send-message irc bot channel
			 (str key " = " (whatmap key)))))
   
   (:cleanup (fn [] (db-close db))))