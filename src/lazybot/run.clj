(ns lazybot.run
  (:use [lazybot core irc info]
        ring.adapter.jetty
        clojure.tools.cli
        [clojure.java.io :only [writer file]])
  (:gen-class))

(defn -main [& args]
  (let [{:keys [logpath background config-dir]}
        (cli args
             (optional
              ["--background"
               "Start lazybot in the background. Should only be used along with --logpath."])
             (optional ["--logpath" "A file for lazybot to direct output to."])
             (optional ["--config-dir" "Directory to look for config.clj and other configuraiton."]))]
    (when config-dir
      (alter-var-root #'*lazybot-dir* (constantly (file config-dir))))
    (if background
      (.exec (Runtime/getRuntime)
             (str "java -jar lazybot.jar --logpath " logpath))
      (let [write (if logpath (writer logpath) *out*)
            config (read-config)]
        (doseq [stream [#'*out* #'*err*]]
          (alter-var-root stream (constantly write)))
        (defonce server (run-jetty #'lazybot.core/sroutes
                                   {:port (:servers-port config) :join? false}))
        (initiate-mongo)
        (doseq [serv (:servers config)]
          (connect-bot #'make-bot serv))
        (route (extract-routes (vals @bots)))))))
