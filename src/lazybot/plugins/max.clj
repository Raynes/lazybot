(ns lazybot.plugins.max
  (:use lazybot.registry
        [somnium.congomongo :only [fetch-one insert! destroy!]]))

(defplugin
  (:hook
   :on-join
   (fn [{:keys [com nick channel]}]
     (future
      (when (= nick (:name @com))
        (Thread/sleep 3000))
      (let [user-count (count (keys (get-in @com [:channels channel :users])))
            max (:max (fetch-one :max :where {:channel channel}))]
        (when (or (not max) (> user-count max))
          (destroy! :max {:channel channel})
          (insert! :max {:channel channel :max user-count}))))))

  (:cmd
   "Find out what the most users ever in this channel at any one time is."
   #{"max"}
   (fn [{:keys [channel] :as com-m}]
     (send-message com-m
                   (str "The most users ever in " channel " is "
                        (:max (fetch-one :max :where {:channel channel}))))))
  (:indexes [[:channel]]))