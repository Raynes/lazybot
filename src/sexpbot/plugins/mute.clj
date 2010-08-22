(ns sexpbot.plugins.mute
  (:use sexpbot.respond))

(defplugin
  (:add-hook
   :on-send-message
   (fn [irc channel s] (when (not (some #(= channel %) (-> irc :configs :mute :channels))) s)))

  (:mute
   "Mutes the bot for the channel that this function is executed in."
   ["mute"] 
   [{:keys [irc nick channel] :as irc-map}]
   (if-admin
    nick irc-map
    (do
      (send-message irc channel "Muting.")
      (dosync (alter irc update-in [:configs :mute :channels] conj channel)))))
  
  (:unmute
   "Unmutes a channel that has been previously muted by :mute."
   ["unmute"]
   [{:keys [irc nick channel] :as irc-map}]
   (if-admin
    nick irc-map
    (do
      (dosync
       (alter irc update-in [:configs :mute :channels]
              (fn [x] (remove #(= % channel) x))))
      (send-message irc channel "Unmuted.")))))