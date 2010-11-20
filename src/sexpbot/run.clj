(ns sexpbot.run
  (:use sexpbot.core
        ring.adapter.jetty
        clojure.contrib.command-line
        [clojure.java.io :only [writer]])
  (:gen-class))

(defn -main [& args]
  (with-command-line args
    "sexpbot -- A Clojure IRC bot"
    [[background? b? "Start sexpbot in the background. Should only be used along with --logpath."]
     [logpath "A file for sexpbot to direct ouput to."]]
    (if background?
      (.exec (Runtime/getRuntime) (str "java -jar sexpbot.jar --logpath " logpath))
      (let [write (if logpath (writer logpath) *out*)]
        (alter-var-root #'*out* (fn [& _] write))
        (alter-var-root #'*err* (fn [& _] write))
        (defonce server (run-jetty #'sexpbot.core/sroutes {:port servers-port :join? false}))
        (require-plugins)
        (doseq [serv (:servers initial-info)] (connect-bot serv))
        (route (extract-routes (vals @bots)))))))