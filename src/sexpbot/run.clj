(ns sexpbot.run
  (:use [sexpbot core load]))

(require-plugins)

(doseq [server (:servers info)]
  (let [bot (make-bot server)]
    (swap! bots assoc server bot)
    (load-plugins bot)
    (doseq [plug (:plugins info)] (load-modules bot))))
