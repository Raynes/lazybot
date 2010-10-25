(ns sexpbot.plugins.seen
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[clj-time core format]
	[somnium.congomongo :only [fetch fetch-one insert! destroy!]]
    [clojure.string :only [join]]))

(defn tack-time
  "Takes a nick and updates the seen database with that nick and the current time."
  [nick server channel doing]
  (let [lower-nick (.toLowerCase nick)]
    (destroy! :seen {:nick nick :server server})
    (insert! :seen
             {:server server
              :time (unparse (formatters :date-time) (now)) 
              :chan channel 
              :doing doing
              :nick nick})))

(defn get-seen
  "Gets the last-seen for a nick."
  [nick server]
  (when-let [seen-map (fetch-one :seen :where {:nick nick :server server})]
    (assoc seen-map :time (in-minutes 
			   (interval (parse (formatters :date-time) (:time seen-map))
				     (now))))))

(defn put-seen [{:keys [nick channel irc]} doing] (tack-time nick (:server @irc) channel doing))

;; This is a bit ugly. Each entry in the table describes how many of the labelled unit
;; it takes to constitute the next-largest unit. It can't be a map because order matters.
(def time-units
     [['second 1]      ; change to 60 if you ever want to show seconds
      ['minute 60]
      ['hour 24]
      ['day 7]
      ['week Integer/MAX_VALUE]]) ; Extend if you want month/year/whatever

(defn decorate [num label]
  (when (> num 0)
    (str num " " label (if (> num 1) "s" ""))))

;; Run through each time unit, finding mod and quotient of the time left.
;; Track the number of minutes left to work on, and the list of decorated values.
;; Note that we build it back-to-front so that larger units end up on the left.
(defn compute-units [minutes]
  (second
   (reduce (fn [[time-left units-so-far] [name ratio]]
             (let [[time-left r] ((juxt quot rem) time-left ratio)]
               [time-left (cons (decorate r name)
                                units-so-far)]))
           [minutes ()] ; Start with no labels, and all the minutes
           time-units)))

;; Now drop out the nil labels, and glue it all together
(defn format-time [minutes]
  (if (= minutes 0)
    "0 minutes"
    (join ", " (->> (compute-units minutes)
                    (drop-while nil?)
                    (take 2) ; If a high-order thing like week is nonzero, don't bother with hours
                    (remove nil?)))))

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
                                          chan " " (format-time time) " ago."))
       (send-message irc bot channel (str "I have never seen " (first args) "."))))))