(ns lazybot.plugins.seen
  (:require [lazybot.registry :as registry]
            [lazybot.info :as info]
            [lazybot.utilities :refer [format-time]]
            [somnium.congomongo :refer [fetch fetch-one insert! destroy!]]
            [clojure.string :refer [join]]))

(defn now []
  (System/currentTimeMillis))

(defn tack-time
  "Takes a nick and updates the seen database with that nick and the current time."
  [nick server channel doing]
  (let [lower-nick (.toLowerCase nick)]
    (destroy! :seen {:nick lower-nick :server server})
    (insert! :seen
             {:server server
              :time (now)
              :chan channel
              :doing doing
              :nick lower-nick})))

(defn get-seen
  "Gets the last-seen for a nick."
  [nick server]
  (when-let [seen-map (fetch-one :seen :where {:nick (.toLowerCase nick)
                                               :server server})]
    (update-in seen-map [:time] #(- (now) %))))

(defn put-seen [{:keys [nick channel com]} doing]
  (tack-time nick (:server @com) channel doing))

(registry/defplugin
  (:hook :on-message
         (fn [irc-map] (put-seen irc-map "talking")))
  (:hook :on-join
         (fn [irc-map] (put-seen irc-map "joining")))
  (:hook :on-quit
         (fn [irc-map] (put-seen irc-map "quitting")))

  (:cmd
   "Checks to see when the person you specify was last seen."
   #{"seen"}
   (fn [{:keys [com bot channel args] :as com-m}]
     (let [[who] args]
       (registry/send-message com-m
                     (if-let [{:keys [time chan doing]}
                              (get-seen who (:server @com))]

                       (str who " was last seen " doing
                            (when-not (= doing "quitting") " on ")
                            chan " " (or (format-time time)
                                         "just moments") " ago.")
                       (str "I have never seen " who "."))))))
  (:index [[:nick :server] :unique true]))
