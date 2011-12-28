(ns lazybot.plugins.weather
  (:use [lazybot registry [utilities :only [shorten-url]]])
  (:require [ororo.core :as w]
            [clojure.string :as string]))

(defn token [bot] (-> bot :config :weather :token))

(defn parse-location [args]
  (if (= 1 (count args))
    (first args)
    [(first args) (second args)]))

(defn generate-forecast [m]
  (str (:title m) ": " (:fcttext m)))

(defn extract-time [k]
  (fn [data]
    (string/join ":" ((juxt :hour :minute) (k data)))))

(defplugin
  (:cmd
   "Get the forecast for a location."
   #{"forecast"}
   (fn [{:keys [bot nick args] :as com-m}]
     (when-let [token (token @bot)]
       (doseq [msg (->> (w/forecast token (parse-location args))
                        :txt_forecast
                        :forecastday
                       (map generate-forecast))]
         (send-message (assoc com-m :channel nick) msg)
         (Thread/sleep 1000)))))

  (:cmd
   "Get astronomy info for a location."
   #{"astronomy"}
   (fn [{:keys [bot nick args] :as com-m}]
     (when-let [token (token @bot)]
       (let [data (w/astronomy token (parse-location args))]
         (send-message
          com-m
          (apply format
                 (str "Percentage of moon illuminated: %s; Age of moon: %s; "
                      "Current time: %s; Sunset: %s; Sunrise: %s.")
                 ((juxt :percentIlluminated :ageOfMoon (extract-time :current_time)
                        (extract-time :sunset) (extract-time :sunrise)) data))))))))