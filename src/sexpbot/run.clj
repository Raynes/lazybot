(ns sexpbot.run
  (:use [sexpbot core load]))

(require-plugins)

(doseq [server (:servers info)]
  (let [[irc refzors] (make-bot server)]
    (swap! bots assoc server {:irc irc :refzors refzors})
    (load-plugins (:server @irc) refzors)
    (doseq [plug (:plugins info)] (load-modules (:server @irc) refzors))))
