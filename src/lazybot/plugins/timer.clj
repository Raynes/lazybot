(ns lazybot.plugins.timer
  (:require [clojure.string :as s]
            [lazybot.utilities :refer [format-time]]
            [lazybot.registry :refer [defplugin send-message]]
            [clj-time.core :as t]
            [me.raynes.moments :as m])
  (:import java.util.concurrent.TimeUnit))

(def running-timers (atom {}))
(def executor (m/executor 10))

(defn time-to-ms [time]
  (t/plus (t/now)
          (t/hours (:hour time))
          (t/minutes (:minute time))
          (t/seconds (:second time))))

(defn task [count spec com-m]
  (m/schedule-at executor (time-to-ms spec)
                 (fn []
                   (send-message com-m (:message spec))
                   (swap! running-timers dissoc count))))

(defn set-timer [spec com-m]
  (swap! running-timers
         (fn [x]
           (let [count (inc (apply max (or (keys x) [0])))]
             (assoc x count {:task (task count spec com-m)
                             :message (:message spec)})))))

(defn parse-message [s nick]
  (let [split-re #"(\d+)[: ](\d+)[: ](\d+)(?: (.+))?"
        [_ hour minute second msg] (re-find split-re s)]
    (-> (zipmap [:hour :minute :second]
                (map #(Long. %) [hour minute second]))
        (assoc :message (or msg nick)))))

(defn pretty-time
  "Format the time left on a timer prettily and what not"
  [timer]
  (format-time (.getDelay timer (TimeUnit/MILLISECONDS))))

(defn format-line
  "Format $timers line."
  [timers]
  (s/join "; "
          (for [[k {m :message, task :task}] timers
                :let [pretty (pretty-time task)]]
            (format "%d: %s (%s remaining)" k (s/join (take 20 m)) pretty))))

(defplugin
  (:cmd
    "Creates a timer. Specify the time as the first argument in h:m:s format."
    #{"timer"}
    (fn [{:keys [args nick] :as com-m}]
      (-> (s/join " " args)
          (parse-message nick)
          (set-timer com-m))
      (send-message com-m "Timer added.")))
  (:cmd
    "List the running timers."
    #{"timers"}
    (fn [com-m]
      (send-message com-m (format-line @running-timers)))))
