(ns lazybot.run
  (:use [lazybot core twitter irc]
        ring.adapter.jetty
        clojure.contrib.command-line
        [clojure.java.io :only [writer]])
  (:gen-class))

(defn -main [& args]
  (with-command-line args
    "lazybot -- A Clojure IRC bot"
    [[background? b? "Start lazybot in the background. Should only be used along with --logpath."]
     [logpath "A file for lazybot to direct output to."]
     [setup-twitter? "Set up your twitter account with lazybot."]]
    (cond
     background? (.exec (Runtime/getRuntime)
                        (str "java -jar lazybot.jar --logpath " logpath))
     setup-twitter? (setup-twitter)
     
     :else
     (let [write (if logpath (writer logpath) *out*)]
       (doseq [stream [#'*out* #'*err*]]
         (alter-var-root stream (constantly write)))
       (defonce server (run-jetty #'lazybot.core/sroutes
                                  {:port servers-port :join? false}))
       (require-plugins)
       (doseq [serv (:servers initial-info)]
         (connect-bot #'make-bot serv))
       (when (:twitter initial-info)
         (connect-bot #'twitter-loop :twitter))
       (route (extract-routes (vals @bots)))))))
