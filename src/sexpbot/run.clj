(ns sexpbot.run
  (:use [sexpbot core load]))

(require-plugins)

(doseq [server (:servers initial-info)]
  (let [[irc refzors] (make-bot server)]
    (swap! bots assoc server {:irc irc :bot refzors})
    (load-plugins irc refzors)))
