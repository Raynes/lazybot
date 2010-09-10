(ns sexpbot.plugins.status
  (:use [sexpbot respond]))

(def statusmsg-map (atom {}))

(defplugin	 
  (:cmd
   "Sets your away status."
   #{"setaway"}
   (fn [{:keys [irc nick channel args] :as irc-map}]
     (swap! statusmsg-map assoc-in [(:server @irc) nick]
            {:status :away 
             :msg (let [msg (.trim
                             (->> args
                                  (interpose " ")
                                  (apply str)))]
                    (if (seq msg)
                      msg
                      "I'm away."))})))
   (:cmd
    "Return from being away."
    #{"return"}
    (fn [{:keys [irc nick channel] :as irc-map}]
      (swap! statusmsg-map assoc (:server @irc) {nick {:status :active :msg ""}})))

   (:cmd
    "Get the status of a user."
    #{"status"}
    (fn [{:keys [irc bot nick channel args] :as irc-map}]
      (let [user (.trim (or (first args) ""))]
        (if (seq user)
          (let [status-m (@statusmsg-map (:server @irc))]
            (if-let [status-map (status-m user)]
              (if (= :away (get status-map :status))
                (send-message irc bot channel (str user " is away: " (status-map :msg)))
                (send-message irc bot channel (str user " is active. ")))
              (send-message irc bot channel (str (first args) " doesn't exist, or hasn't set their status"))))
          (send-message irc bot channel "Who?"))))))