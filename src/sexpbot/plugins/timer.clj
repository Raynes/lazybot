(ns sexpbot.plugins.timer
  (:refer-clojure :exclude [extend])
  (:require [irclj.irclj :as ircb])
  (:use sexpbot.respond
	clj-time.core))

(def running-timers (atom {}))

(defn compose-timer [{:keys [end-time text]}]
  (let [this-moment (now)
	[ehours eminutes eseconds] [(hour this-moment) (minute this-moment) (sec this-moment)]
	ftime (minus end-time (hours ehours) (minutes eminutes) (secs eseconds))
	end-in ((juxt hour minute sec) ftime) ]
    (str text " Ends in: " (apply str (interpose ":" end-in)))))

(defn cap-text [s n]
  (str (apply str (take n s)) (if (< 30 (count s)) "..." "")))

(defplugin
  (:timers 
   "Prints a list of the currently running timers. Execute in PM if you want a full list."
   ["timers"] 
   [{:keys [irc bot nick channel args]}]
   (let [timers (map compose-timer (vals @running-timers))]
     (if (> (count timers) 0)
       (if-let [n-to-show (first args)]
	 (doseq [timer (take n-to-show timers)] (send-message irc bot channel timer))
	 (do
	   (doseq [timer (take 3 timers)] 
	     (send-message irc bot channel timer))
	   (when (> (count timers) 3) (send-message irc bot channel "and more..."))))
       (send-message irc bot channel "No timers are currently running."))))

  (:timer 
   "Create's a timer. You specify the time you want it to run in a 00:00:00 format, and a message
   to print once the timer has run. Once the timer completes, the message will be printed."
   ["timer"] 
   [{:keys [irc bot channel args]}]
   (.start 
    (Thread. 
     (fn []
       (let [ctime (now)
	     [hour minute sec] (map #(Integer/parseInt %) (.split (first args) ":"))
	     newt (plus ctime (hours hour) (minutes minute) (secs sec))
	     fint (in-secs (interval ctime newt))
	     text (->> args rest (interpose " ") (apply str))
	     timer-name (gensym)]
	 (swap! running-timers assoc timer-name {:end-time newt 
                                                 :text (cap-text text 30)})
	 (Thread/sleep (* fint 1000))
	 (if (= (second args) "/me") 
	   (ircb/send-action irc channel (apply str (interpose " " (nnext args))))
	   (send-message irc bot channel text))
         (swap! running-timers dissoc timer-name)))))))
