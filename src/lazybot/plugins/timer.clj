(ns lazybot.plugins.timer
  (:refer-clojure :exclude [extend])
  (:use lazybot.registry
        clj-time.core)
  (:require [clojure.string :as s]))

(def running-timers (atom {}))

(defn cap-text [s n]
  (str (apply str (take n s)) (if (< 30 (count s)) "..." "")))

(defn compose-timer [[timer-n {:keys [end-time text]}]]
  (let [this-moment (now)
        [ehours eminutes eseconds] [(hour this-moment) (minute this-moment) (sec this-moment)]
        ftime (minus end-time (hours ehours) (minutes eminutes) (secs eseconds))
        end-in ((juxt hour minute sec) ftime) ]
    (str timer-n ": " (cap-text text 20) " ... Ends in: " (apply str (interpose ":" end-in)))))

(defplugin
  (:cmd
   "Prints a list of the currently running timers. Execute in PM if you want a full list."
   #{"timers"}
   (fn [{:keys [bot nick channel args] :as com-m}]
     (let [timers (map compose-timer @running-timers)]
       (if (> (count timers) 0)
         (if-let [n-to-show (first args)]
           (doseq [timer (take n-to-show timers)] (send-message com-m timer))
           (do
             (doseq [timer (take 3 timers)]
               (send-message com-m timer))
             (when (> (count timers) 3) (send-message com-m "and more..."))))
         (send-message com-m "No timers are currently running.")))))

  (:cmd
   "Deletes a timer."
   #{"rmtimer"}
   (fn [{:keys [bot channel args] :as com-m}]
     (swap! running-timers dissoc (Integer/parseInt (first args)))
     (send-message com-m "Timer deleted.")))

  (:cmd
   "Creates a timer. You specify the time you want it to run in a 0:0:0 format, and a message
   to print once the timer has run. Once the timer completes, the message will be printed."
   #{"timer"}
   (fn [{:keys [bot channel args nick] :as com-m}]
     (let [ctime (now)
           args (-> args (->> (s/join " ")) (s/split #"[: ]+"))
           [time message] (split-at 3 args)
           [hour minute sec] (map #(Integer/parseInt %) time)
           newt (plus ctime (hours hour) (minutes minute) (secs sec))
           fint (in-secs (interval ctime newt))
           text (->> (or (seq message)
                         [(str nick ": ping")])
                     (s/join " "))
           n-timers (-> @running-timers keys (or [0]) (->> (apply max)) inc)]
       (swap! running-timers assoc n-timers
              {:end-time newt
               :text text})
       (send-message com-m "Timer added.")
       (future
         (Thread/sleep (* 1000 fint))
         (apply send-message com-m
                (if (= (first message) "/me")
                  [(s/join " " (rest message)) :action? true]
                  [text]))
         (swap! running-timers dissoc n-timers))))))
