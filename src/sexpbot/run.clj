(ns sexpbot.run
  (:use [sexpbot core respond]))

(def bots (ref {}))

; Require all plugin files listed in info.clj
(require-plugins)

(doseq [server (:servers info)]
  (let [bot (make-bot server)]
    (dosync (alter bots assoc server bot))
    (load-plugins bot)))

(doseq [irc (vals @bots)] (println (:hooks @irc)))

(doseq [plug (:plugins info) irc (vals @bots)] (.start (Thread. (fn [] (loadmod irc plug)))))

