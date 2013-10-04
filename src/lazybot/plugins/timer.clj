(ns lazybot.plugins.timer
  (:require [clojure.string :as s]
            [lazybot.registry :refer [defplugin send-message]]
            [clj-time.core :as t]
            [me.raynes.moments :as m]))

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
                   (swap! running-timers
                          #(-> %
                               (update-in [:count] dec)
                               (dissoc count))))))

(defn set-timer [spec com-m]
  (swap! running-timers
         (fn [x]
           (let [count (inc (:count x 0))]
             (assoc x
                    :count count
                    count (task count spec com-m))))))

(defn parse-message [s]
  (let [[offset message] (s/split s #" " 2)]
    (-> (zipmap [:hour :minute :second]
                (map #(Long. %) (s/split offset #":")))
        (assoc :message message))))

(defplugin
  (:cmd
    "Creates a timer. Specify the time as the first argument in h:m:s format."
    #{"timer"}
    (fn [{:keys [args] :as com-m}]
      (-> (s/join " " args)
          (parse-message)
          (set-timer com-m))
      (send-message com-m "Timer added."))))
