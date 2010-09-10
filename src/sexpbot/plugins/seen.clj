(ns sexpbot.plugins.seen
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[clj-time core format]
	[somnium.congomongo :only [fetch fetch-one insert! destroy!]]))

(defn tack-time
  "Takes a nick and updates the seen database with that nick and the current time."
  [nick server channel doing]
  (let [lower-nick (.toLowerCase nick)]
    (insert! :seen
             {:server server
              :time (unparse (formatters :date-time) (now)) 
              :chan channel 
              :doing doing
              :nick nick})))

(defn get-seen
  "Get's the last-seen for a nick."
  [nick server]
  (when-let [seen-map (fetch-one :seen :where {:nick nick :server server})]
    (assoc seen-map :time (in-minutes 
			   (interval (parse (formatters :date-time) (:time seen-map))
				     (now))))))

(defn put-seen [{:keys [nick channel irc]} doing] (tack-time nick (:server @irc) channel doing))

(defplugin
  (:hook :on-message
         (fn [irc-map] (put-seen irc-map "talking")))
  (:hook :on-join 
         (fn [irc-map] (put-seen irc-map "joining")))
  (:hook :on-quit
         (fn [irc-map] (put-seen irc-map "quitting")))
  
  (:cmd
   "Checks to see when the person you specify was last seen."
   #{"seen"} 
   (fn [{:keys [irc bot nick channel args]}]
     (if-let [{:keys [time chan doing nick]} (get-seen (first args) (:server @irc))]
       (send-message irc bot channel (str nick " was last seen " doing (when-not (= doing "quitting") " on ") 
                                          chan " " time " minutes ago."))
       (send-message irc bot channel (str "I have never seen " (first args) "."))))))