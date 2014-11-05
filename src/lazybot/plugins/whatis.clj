(ns lazybot.plugins.whatis
  (:require [lazybot.registry :as registry]
            [somnium.congomongo :refer [fetch fetch-one insert! destroy!]]))

(defn tell-about [what com-m]
  (registry/send-message com-m
                (str what
                     (if-let [result (fetch-one :whatis :where {:subject what})]
                       (str " is " (:is result))
                       " does not exist in my database."))))

(registry/defplugin
  (:cmd
   "Teaches the bot a new thing. It takes a name and whatever you want to assign the name
   to. For example: $learn me a human being."
   #{"learn"} 
   (fn [{:keys [args] :as com-m}]
     (let [[subject & is] args
           is-s (apply str (interpose " " is))]
       (do
         (destroy! :whatis {:subject subject})
         (insert! :whatis {:subject subject :is is-s})
         (registry/send-message com-m "My memory is more powerful than M-x butterfly. I won't forget it.")))))
   
   (:cmd 
    "Pass it a key, and it will tell you what is at the key in the database."
    #{"whatis"}
    (fn [{[what] :args :as com-m}]
      (tell-about what com-m)))

   (:cmd 
    "Pass it a key, and it will tell the recipient what is at the key in the database via PM
Example - $tell G0SUB about clojure"
    #{"tell"}
    (fn [{[who _ what] :args :as com-m}]
      (when what
        (tell-about what (assoc com-m :channel who)))))
   
   (:cmd 
    "Forgets the value of a key."
    #{"forget"} 
    (fn [{[what] :args :as com-m}]
      (do (destroy! :whatis {:subject what})
          (registry/send-message com-m (str "If " what " was there before, it isn't anymore. R.I.P.")))))

   (:cmd 
    "Gets a random value from the database."
    #{"rwhatis"} 
    (fn [com-m]
      (let [all (-> :whatis fetch)
            what (and (not-empty all) (-> all rand-nth :subject))]
        (tell-about what com-m))))
   (:indexes [[:subject]]))
