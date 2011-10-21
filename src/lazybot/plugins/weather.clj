(ns lazybot.plugins.weather
  (:use ororo.core
        [lazybot registry [utilities :only [shorten-url]]]))

(defn token [bot] (-> bot :config :weather :token))

(defn parse-location [args]
  (if (= 1 (count args))
    (first args)
    [(first args) (second args)]))

(defn generate-forecast [m]
  (str (:title m) ": " (:fcttext m)))

(defplugin
  (:cmd
   "Get the forecast for a location."
   #{"forecast"}
   (fn [{:keys [bot nick args] :as com-m}]
     (when-let [token (token @bot)]
       (doseq [msg (->> (forecast token (parse-location args))
                        :txt_forecast
                        :forecastday
                       (map generate-forecast))]
         (send-message (assoc com-m :channel nick) msg)
         (Thread/sleep 1000))))))