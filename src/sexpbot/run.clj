(ns sexpbot.run
  (:use [sexpbot core utilities info respond])
  (:require[irclj.irclj :as ircb]))

(def bots (ref {}))

; Require all plugin files listed in info.clj
(reload-plugins)

(doseq [plug (:plugins (read-config))] (.start (Thread. (fn [] (loadmod plug)))))
(doseq [server (:servers (read-config))]
  (dosync (alter bots assoc server (make-bot server))))