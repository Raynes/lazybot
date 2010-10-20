(ns sexpbot.plugins.max
  (:use sexpbot.respond
        [somnium.congomongo :only [fetch-one insert! destroy!]]))

(defplugin
  (:hook
   :on-join
   (fn [{:keys [irc nick channel]}]
     (future
      (when (= nick (:name @irc))
        (Thread/sleep 3000))
      (println (keys (get-in @irc [:channels channel :users])))
      (let [user-count (count (keys (get-in @irc [:channels channel :users])))
            max (:max (fetch-one :max :where {:channel channel}))]
        (println user-count)
        (when (or (not max) (> user-count max))
          (destroy! :max {:channel channel})
          (insert! :max {:channel channel :max user-count}))))))

  (:cmd
   "Find out what the most users ever in this channel at any one time is."
   #{"max"}
   (fn [{:keys [irc bot channel]}]
     (send-message irc bot channel
                   (str "The most users ever in " channel " is "
                        (:max (fetch-one :max :where {:channel channel})))))))