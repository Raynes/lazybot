(ns sexpbot.plugins.whatis
  (:use [sexpbot respond]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin 
  (:learn 
   "Teaches the bot a new thing. It takes a name and whatever you want to assign the name
   to. For example: $learn me a human being."
   ["learn"] 
   [{:keys [irc bot channel args]}]
   (let [[subject & is] args
	 is-s (apply str (interpose " " is))]
     (do
       (destroy! :whatis {:subject subject})
       (insert! :whatis {:subject subject :is is-s})
       (send-message irc bot channel "My memory is more powerful than M-x butterfly. I wont forget it."))))
   
   (:whatis 
    "Pass it a key, and it will tell you what is at the key in the database."
    ["whatis"]
    [{:keys [irc bot channel args]}]
    (if-let [result (fetch-one :whatis :where {:subject (first args)})]
      (send-message irc bot channel (str (first args) " = " (:is result)))
      (send-message irc bot channel (str (first args) " does not exist in my database."))))
   
   (:forget 
    "Forgets the value of a key."
    ["forget"] 
    [{:keys [irc bot channel args]}]
    (do (destroy! :whatis {:subject (first args)})
      (send-message irc bot channel (str "If " (first args) " was there before, it isn't anymore. R.I.P."))))

   (:rwhatis 
    "Gets a random value from the database."
    ["rwhatis"] 
    [{:keys [irc bot channel]}]
    (let [rand-subject (rand-nth (fetch :whatis))]
      (send-message irc bot channel
                    (str (:subject rand-subject) " = " (:is rand-subject))))))