(ns sexpbot.plugins.seen
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[clj-time core format])
  (:require [irclj.irclj :as ircb]))

(def seenfile (str sexpdir "/seen.clj"))

(defn tack-time
  "Takes a nick and updates the seen database with that nick and the current time."
  [nick channel doing]
  (let [lower-nick (.toLowerCase nick)]
    (with-info seenfile
      (-> (read-config) (assoc lower-nick {:time (unparse (formatters :date-time) (now)) 
					   :chan channel 
					   :doing doing
					   :nick nick}) 
	  (write-config)))))

(defn get-seen
  "Get's the last-seen for a nick."
  [nick]
  (with-info seenfile
    (when-let [seen-map ((read-config) (.toLowerCase nick))]
      (assoc seen-map :time (in-minutes 
			     (interval (parse (formatters :date-time) (:time seen-map))
				       (now)))))))

(defmethod respond :seen [{:keys [irc nick channel args]}]
  (if-let [{:keys [time chan doing nick]} (get-seen (first args))]
    (ircb/send-message irc channel (str nick " was last seen " doing (when-not (= doing "quitting") " on ") 
				    chan " " time " minutes ago."))
    (ircb/send-message irc channel (str "I have never seen " (first args) "."))))

(defmethod respond :putseen* [{:keys [nick channel extra-args]}]
  (tack-time nick channel (first extra-args)))

(defplugin
  {"putseen*" :putseen*
   "seen"     :seen})
