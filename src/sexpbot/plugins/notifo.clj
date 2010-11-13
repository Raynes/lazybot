; Written by Michael D. Ivey <ivey@gweezlebur.com>
; Licensed under the EPL

; Usage:
; * Go to http://notifo.com and register an account
; * Add a section to info.clj at the top level like this
;    :notifo {-Service Name- {-Title- {-Server- [-chans-]}}}
; * For example, for push.ly notifications from Twitter user @ExampleUser
;     :notifo {"Push.ly" {"DM - @ExampleUser:" {"irc.freenode.net" ["#tempchan"]}}}
; * At notifo.com, click Settings -> Notification Settings and enter
;   http://your-server-ip:8080/notifo as your Notification Webhook URL
; * Enjoy

(ns sexpbot.plugins.notifo
  (:use sexpbot.plugin
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