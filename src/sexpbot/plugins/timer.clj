(ns sexpbot.plugins.timer
  (:use sexpbot.respond
	[clj-time core]))

(def running-timers (ref {}))

(defn compose-timer [{:keys [end-time text]}]
  (let [this-moment (now)
	[ehours eminutes eseconds] [(hour this-moment) (minute this-moment) (sec this-moment)]
	ftime (minus end-time (hours ehours) (minutes eminutes) (secs eseconds))
	end-in ((juxt hour minute sec) ftime) ]
    (str text " Ends in: " (apply str (interpose ":" end-in)))))

(defmethod respond :timers [{:keys [bot sender channel args]}]
  (let [timers (map compose-timer (vals @running-timers))]
    (if (> (count timers) 0)
      (if-let [n-to-show (first args)]
	(doseq [timer (take n-to-show timers)] (.sendMessage bot channel timer))
	(do
	  (doseq [timer (take 3 timers)] 
	    (.sendMessage bot channel timer))
	  (when (> (count timers) 3) (.sendMessage bot channel "and more..."))))
      (.sendMessage bot channel "No timers are currently running."))))

(defn cap-text [s n]
  (str (apply str (take n s)) (if (< 30 (count s)) "..." "")))

(defmethod respond :timer [{:keys [bot channel args]}]
  (.start 
   (Thread. 
    (fn []
      (let [ctime (now)
	    [hour minute second] (map #(Integer/parseInt %) (.split (first args) ":"))
	    newt (plus ctime (hours hour) (minutes minute) (secs second))
	    fint (in-secs (interval ctime newt))
	    text (->> args rest (interpose " ") (apply str))
	    timer-name (gensym)]
	(dosync (alter running-timers assoc timer-name {:end-time newt 
							:text (cap-text text 30)}))
	(Thread/sleep (* fint 1000))
	(->> text (.sendMessage bot channel))
	(dosync (alter running-timers dissoc timer-name)))))))

(defplugin
  {"timer"  :timer
   "timers" :timers})