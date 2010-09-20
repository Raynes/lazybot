(ns sexpbot.plugins.notifo
  (:use sexpbot.respond
        [compojure.core :only [POST]]))

(def bots (atom {}))

(defn grab-config [] (-> @bots vals first :bot deref :config))

(defn handler [req]
  (let [data (:form-params req)
        config (:notifo (grab-config))
        service (data "notifo_service")
        title (data "notifo_title")
        message (data "notifo_message")]
    (when-let [conf ((config service) title)]
      (doseq [[server channels] conf]
        (let [{:keys [irc bot]} (@bots server)]
          (doseq [chan channels]
            (send-message irc bot chan message))))))
  "These boots are made for walkin' and that's just what they'll do.")

(defplugin
  (:init
   (fn [irc bot]
     (swap! bots assoc (:server @irc) {:irc irc :bot bot})))
  (:routes (POST "/notifo" req (handler req))))