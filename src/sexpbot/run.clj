(ns sexpbot.run
  (:use [sexpbot core]))

(require-plugins)

(doseq [server (:servers initial-info)] (connect-bot server))
(prn (vals @bots))
(route (extract-routes (vals @bots)))