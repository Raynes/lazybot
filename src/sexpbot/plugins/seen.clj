(ns sexpbot.plugins.seen
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info utilities]
	[clj-time core format]
	stupiddb.core)
  (:require [irclj.irclj :as ircb]))


(def seenfile (str sexpdir "/seen.db"))
(def db (db-init seenfile 1800))


(defn tack-time
  "Takes a nick and updates the seen database with that nick and the current time."
  [nick channel doing]
  (let [lower-nick (.toLowerCase nick)]
    (db-assoc db lower-nick {:time (unparse (formatters :date-time) (now)) 
					   :chan channel 
					   :doing doing
					   :nick nick})))

(defn get-seen
  "Get's the last-seen for a nick."
  [nick]
    (when-let [seen-map (db-get db (.toLowerCase nick))]
      (assoc seen-map :time (in-minutes 
			(interval (parse (formatters :date-time) (:time seen-map))
				  (now))))))

(defn put-seen [{:keys [nick channel]} doing] (tack-time nick channel doing))

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
   [{:keys [irc nick channel args]}]
   (if-let [{:keys [time chan doing nick]} (get-seen (first args))]
     (ircb/send-message irc channel (str nick " was last seen " doing (when-not (= doing "quitting") " on ") 
					 chan " " time " minutes ago."))
     (ircb/send-message irc channel (str "I have never seen " (first args) "."))))
  (:cleanup (fn [] (db-close db))))