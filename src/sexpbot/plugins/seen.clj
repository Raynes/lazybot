(ns sexpbot.plugins.seen
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[clj-time core format]
	stupiddb.core))

(def seenfile (str sexpdir "/seen.db"))
(def db (db-init seenfile 1800))

(defn tack-time
  "Takes a nick and updates the seen database with that nick and the current time."
  [nick server channel doing]
  (let [lower-nick (.toLowerCase nick)]
    (db-assoc-in db [server lower-nick] 
		 {:time (unparse (formatters :date-time) (now)) 
		  :chan channel 
		  :doing doing
		  :nick nick})))

(defn get-seen
  "Get's the last-seen for a nick."
  [nick server]
  (when-let [seen-map (db-get-in db [server (.toLowerCase nick)])]
    (assoc seen-map :time (in-minutes 
			   (interval (parse (formatters :date-time) (:time seen-map))
				     (now))))))

(defn put-seen [{:keys [nick channel irc]} doing] (tack-time nick (:server @irc) channel doing))

(defplugin
  (:add-hook :on-message
	     (fn [irc-map] (put-seen irc-map "talking")))
  (:add-hook :on-join 
	     (fn [irc-map] (put-seen irc-map "joining")))
  
  (:add-hook :on-quit
	     (fn [irc-map] (put-seen irc-map "quitting")))
  
  (:seen 
   "Checks to see when the person you specify was last seen."
   ["seen"] 
   [{:keys [irc bot nick channel args]}]
   (if-let [{:keys [time chan doing nick]} (get-seen (first args) (:server @irc))]
     (send-message irc bot channel (str nick " was last seen " doing (when-not (= doing "quitting") " on ") 
                                        chan " " time " minutes ago."))
     (send-message irc bot channel (str "I have never seen " (first args) "."))))
  (:cleanup (fn [] (db-close db))))