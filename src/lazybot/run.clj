(ns lazybot.run
  (:require [lazybot.core :as lazybot]
            [lazybot.irc :as irc]
            [lazybot.info :as info]
            [clojure.tools.cli :as cli]
        [clojure.java.io :refer [writer file]])
  (:gen-class))

(defn -main [& args]
  (let [{:keys [logpath background config-dir]}
        (cli/cli args
                 (cli/optional
                  ["--background"
                   "Start lazybot in the background. Should only be used along with --logpath."])
                 (cli/optional ["--logpath" "A file for lazybot to direct output to."])
                 (cli/optional ["--config-dir" "Directory to look for config.clj and other configuraiton."]))]
    (when config-dir
      (alter-var-root #'info/*lazybot-dir* (constantly (file config-dir))))
    (if background
      (.exec (Runtime/getRuntime)
             (str "java -jar lazybot.jar --logpath " logpath))
      (let [write (if logpath (writer logpath) *out*)
            config (info/read-config)]
        (doseq [stream [#'*out* #'*err*]]
          (alter-var-root stream (constantly write)))
        (lazybot/start-server (:servers-port config 8080))
        (lazybot/initiate-mongo)
        (irc/start-bots (:servers config))))))
