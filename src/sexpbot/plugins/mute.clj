(ns sexpbot.plugins.mute
  (:use sexpbot.registry
        [sexpbot.plugins.login :only [when-privs]]))

(defplugin :irc
  (:hook
   :on-send-message
   (fn [_ bot channel s action?] (when-not (some #{channel} (-> @bot :configs :mute :channels)) s)))

  (:cmd
   "Mutes the bot for the channel that this function is executed in."
   #{"mute"} 
   (fn [{:keys [bot nick channel] :as com-m}]
     (when-privs com-m :admin
      (do
        (send-message com-m "Muting.")
        (dosync (alter bot update-in [:configs :mute :channels] conj channel))))))
  
  (:cmd
   "Unmutes a channel that has been previously muted by :mute."
   #{"unmute"}
   (fn [{:keys [bot nick channel] :as com-m}]
     (when-privs com-m :admin
      (do
        (dosync
         (alter bot update-in [:configs :mute :channels]
                (fn [x] (remove #(= % channel) x))))
        (send-message com-m "Unmuted."))))))