(ns lazybot.plugins.lastfm
  (:require [me.raynes.least :as least]
            [clojure.java.io :refer [file]]
            [clojure.string :refer [join]]
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
    (prn assocs)))

(defn update-agent [state server nick user]
  (doto (assoc-in state [server nick] user)
    (write-associations)))

(defn add-assoc [server nick user]
  (send-off associations update-agent server nick user))

(defn get-api-key [bot]
  (get-in @bot [:config :lastfm :api-key]))

(defn get-association [server nick & [not-found]]
  (get-in @associations [server nick] not-found))

(defn first-if-sequential [x]
  (if (sequential? x) (first x) x))

(defn get-latest-song [bot server nick]
  (let [user (get-association server nick nick)]
    (when-let [latest (first-if-sequential (get-in (least/read "user.getRecentTracks"
                                                               (get-api-key bot)
                                                               {:user user 
                                                                :limit 1})
                                                   [:recenttracks :track]))]
      (format "%s %s: %s - %s [%s]"
              nick
              (if (= "true" (get-in latest [:attr :nowplaying]))
                "is listening to"
                "last listened to")
              (get-in latest [:artist :text])
              (:name latest)
              (get-in latest [:album :text])))))

(defn get-top [kind bot server nick cull & [period]]
  (when-let [user (get-association server nick nick)]
    (when-let [targets
               (not-empty
                (for [{:keys [name playcount]}
                      (get-in (least/read (str "user.getTop" kind)
                                          (get-api-key bot)
                                          {:user user 
                                           :limit 5
                                           :period (or period "overall")})
                              cull)]
                  (str name " " playcount)))]
      (join " | " targets))))

(defplugin
  (:cmd
    "Get the latest song played by a user (yourself by default)."
    #{"last"}
    (fn [{:keys [nick network bot args] :as com-m}]
      (send-message
        com-m
        (or (get-latest-song bot network (or (first args) nick))
            "Couldn't find that user."))))

  (:cmd
    "Get the top artists a user has listened to. By default, the aggregation is overall,
    but you can change the period. Pass a second arg that is any of 7day, 1month, 3month,
    6month, or 12month."
    #{"topartists"}
    (fn [{:keys [network bot args] :as com-m}]
      (send-message
        com-m
        (or (get-top "Artists" bot network (first args) [:topartists :artist] (second args))
            "Couldn't find that user."))))

  (:cmd
    "Get the top tracks a user has listened to. By default, the aggregation is overall,
    but you can change the period. Pass a second arg that is any of 7day, 1month, 3month,
    6month, or 12month."
    #{"toptracks"}
    (fn [{:keys [network bot args] :as com-m}]
      (send-message
        com-m
        (or (get-top "Tracks" bot network (first args) [:toptracks :track] (second args))
            "Couldn't find that user."))))

  (:cmd
    "Get the top albums a user has listened to. By default, the aggregation is overall,
    but you can change the period. Pass a second arg that is any of 7day, 1month, 3month,
    6month, or 12month."
    #{"topalbums"}
    (fn [{:keys [network bot args] :as com-m}]
      (send-message
        com-m
        (or (get-top "Albums" bot network (first args) [:topalbums :album] (second args))
            "Couldn't find that user."))))
  
  (:cmd
    "Associate your nickname with a lastfm username"
    #{"lfmassoc"}
    (fn [{:keys [nick network args] :as com-m}]
      (send-message
        com-m
        (if-let [user (first args)]
          (do (add-assoc network nick user)
              "Associated your username.")
          "Well, I can't guess your username. I need an argument, please."))))
  
  (:cmd
    "Find out what username is associated with a given nickname."
    #{"lfmuser"}
    (fn [{:keys [network args] :as com-m}]
      (send-message
        com-m
        (if-let [user (get-association network (first args))]
          (str "http://last.fm/user/" user)
          "User not found.")))))
