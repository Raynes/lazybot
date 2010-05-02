(ns sexpbot.plugins.seen
  (:refer-clojure :exclude [extend])
  (:use [sexpbot respond info]
	[clj-time core format])
  (:require [irclj.irclj :as ircb]))

;; TODO: Port to StupidDB


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

(defplugin
  (:add-hook :on-message
	     (fn [irc-map]
	       (try-handle (assoc irc-map
			     :message (str (:prepend (read-config)) "putseen*")
			     :extra-args ["talking"]))))
  (:add-hook :on-join 
	     (fn [irc-map] 
	       (try-handle (assoc irc-map 
			     :message (str (:prepend (read-config)) "putseen*")
			     :extra-args ["joining"]))))

  (:add-hook :on-quit
	     (fn [irc-map] 
	       (try-handle (assoc irc-map 
			     :message (str (:prepend (read-config)) "putseen*")
			     :extra-args ["quitting"]))))

  (:seen 
   "Checks to see when the person you specify was last seen."
   ["seen"] 
   [{:keys [irc nick channel args]}]
   (if-let [{:keys [time chan doing nick]} (get-seen (first args))]
     (ircb/send-message irc channel (str nick " was last seen " doing (when-not (= doing "quitting") " on ") 
					 chan " " time " minutes ago."))
     (ircb/send-message irc channel (str "I have never seen " (first args) "."))))

  (:putseen* "" ["putseen*"] [{:keys [nick channel extra-args]}] (tack-time nick channel (first extra-args))))