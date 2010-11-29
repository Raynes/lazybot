(ns sexpbot.plugins.whatis
  (:use [sexpbot registry]
        [somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defplugin 
  (:cmd
   "Teaches the bot a new thing. It takes a name and whatever you want to assign the name
   to. For example: $learn me a human being."
   #{"learn"} 
   (fn [{:keys [bot channel args] :as com-m}]
     (let [[subject & is] args
           is-s (apply str (interpose " " is))]
       (do
         (destroy! :whatis {:subject subject})
         (insert! :whatis {:subject subject :is is-s})
         (send-message com-m "My memory is more powerful than M-x butterfly. I wont forget it.")))))
   
   (:cmd 
    "Pass it a key, and it will tell you what is at the key in the database."
    #{"whatis"}
    (fn [{:keys [bot channel args] :as com-m}]
      (if-let [result (fetch-one :whatis :where {:subject (first args)})]
        (send-message com-m (str (first args) " = " (:is result)))
        (send-message com-m (str (first args) " does not exist in my database.")))))
   
   (:cmd 
    "Forgets the value of a key."
    #{"forget"} 
    (fn [{:keys [bot channel args] :as com-m}]
      (do (destroy! :whatis {:subject (first args)})
          (send-message com-m (str "If " (first args) " was there before, it isn't anymore. R.I.P.")))))

   (:cmd 
    "Gets a random value from the database."
    #{"rwhatis"} 
    (fn [{:keys [bot channel] :as com-m}]
      (let [rand-subject (rand-nth (fetch :whatis))]
        (send-message com-m (str (:subject rand-subject) " = " (:is rand-subject)))))))