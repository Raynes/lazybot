(ns sexpbot.run
  (:use [sexpbot core respond]))

(def bots (ref {}))

; Require all plugin files listed in info.clj
(reload-plugins)

(doseq [plug (:plugins info)] (.start (Thread. (fn [] (loadmod plug)))))
(doseq [server (:servers info)]
  (dosync (alter bots assoc server (make-bot server))))