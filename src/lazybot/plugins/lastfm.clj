(ns lazybot.plugins.lastfm
  (:require [me.raynes.least :as least]
            [clojure.java.io :refer [file]]
            [clojure.tools.reader.edn :as edn]
            [lazybot.info :refer [*lazybot-dir*]]
            [lazybot.registry :refer [send-message defplugin]])
  (:import java.io.FileWriter))

(def association-file
  (file *lazybot-dir* "lastfmassociations.clj"))

(when-not (.exists association-file)
  (.createNewFile association-file))

(def associations (agent (or (edn/read-string (slurp association-file))
                             {})))

(defn write-associations [assocs]
  (binding [*out* (FileWriter. association-file)]
    (pr-str assocs)))

(defn update-agent [state server nick user]
  (doto (assoc-in state [server nick] user)
    (write-associations)))

(defn add-assoc [server nick user]
  (send-off associations update-agent server nick user))

(defn get-api-key [bot]
  (get-in @bot [:config :lastfm :api-key]))

(defn get-latest-song [bot server nick]
  (let [user (get-in @associations [server nick] nick)]
    (when-let [latest (first (get-in (least/read "user.getRecentTracks"
                                                 (get-api-key bot)
                                                 {:user user 
                                                  :limit 1})
                                     [:recenttracks :track]))]
      (format "%s %s: %s - %s [%s]"
              user
              (if (= "true" (get-in latest [:attr :nowplaying]))
                "is listening to"
                "last listened to")
              (get-in latest [:artist :text])
              (:name latest)
              (get-in latest [:album :text])))))

(defplugin
  (:cmd
    "Get the latest song played by a user (yourself by default)."
    #{"last"}
    (fn [{:keys [nick com bot args] :as com-m}]
      (send-message
        com-m
        (or (get-latest-song bot (:server @com) (or (first args) nick))
            "Couldn't find that user."))))
  
  (:cmd
    "Associate your nickname with a lastfm username"
    #{"lfmassoc"}
    (fn [{:keys [nick com args] :as com-m}]
      (send-message
        com-m
        (if-let [user (first args)]
          (do (add-assoc (:server @com) nick user)
              "Associated your username.")
          "Well, I can't guess your username. I need an argument, please.")))))
