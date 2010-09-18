(ns sexpbot.run
  (:use [sexpbot core]))

(require-plugins)

(doseq [server (:servers initial-info)] (connect-bot server))
(route (extract-routes (vals @bots)))