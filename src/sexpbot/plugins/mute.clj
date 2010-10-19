(ns sexpbot.plugins.mute
  (:use sexpbot.respond))

(defplugin
  (:hook
   :on-send-message
   (fn [_ bot channel s action?] (when (not (some #(= channel %) (-> @bot :configs :mute :channels))) s)))

  (:cmd
   "Mutes the bot for the channel that this function is executed in."
   #{"mute"} 
   (fn [{:keys [irc bot nick channel] :as irc-map}]
     (if-admin
      nick irc-map bot
      (do
        (send-message irc bot channel "Muting.")
        (dosync (alter bot update-in [:configs :mute :channels] conj channel))))))
  
  (:cmd
   "Unmutes a channel that has been previously muted by :mute."
   #{"unmute"}
   (fn [{:keys [irc bot nick channel] :as irc-map}]
     (if-admin
      nick irc-map bot
      (do
        (dosync
         (alter bot update-in [:configs :mute :channels]
                (fn [x] (remove #(= % channel) x))))
        (send-message irc bot channel "Unmuted."))))))