(ns sexpbot.run
  (:use sexpbot.core ring.adapter.jetty)
  (:gen-class))

(defn -main [& args]
  (defonce server (run-jetty #'sexpbot.core/sroutes {:port servers-port :join? false}))
  (require-plugins)
  (doseq [server (:servers initial-info)] (connect-bot server))
  (route (extract-routes (vals @bots))))