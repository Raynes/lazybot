(ns sexpbot.plugins.github
  (:use sexpbot.registry
        [sexpbot.plugins.shorturl :only [is-gd]]
        [compojure.core :only [POST]]
        clojure.contrib.json)
  (:require [clojure.contrib.string :as s])
  (:import java.net.InetAddress))

(def bots (atom {}))

(extend nil Read-JSON-From {:read-json-from (constantly nil)})

(defn grab-config [] (-> @bots vals first :bot deref :config))

(defn format-vec [v]
  (let [[show hide] (split-at 10 v)]
    (s/join "" ["[" (s/join ", " show)
              (when (seq hide) "...") "]"])))

(defn notify-chan [com-m commit owner name branch no-header]
  (send-message
   com-m
   (let [{:keys [added removed modified]} commit]
     (s/join "" [(when no-header
                   (str "\u0002" owner "/" name "\u0002: " branch " <" (is-gd (:url commit)) "> "))
                 "\u0002" (-> commit :author :name) "\u0002: "
                 (format-vec (concat modified (map #(str "+" %) added) (map #(str "-" %) removed)))
                 " \u0002--\u0002 " (:message commit)]))))

(defn handler [req]
  (let [remote (:remote-addr req)]
    (when (or (= "127.0.0.1" remote)
              (.endsWith (.getCanonicalHostName (InetAddress/getByName remote)) "github.com")))
    (let [{:keys [before repository commits after compare ref] :as payload}
          (read-json ((:form-params req) "payload"))
          config (:commits (grab-config))]
      (when payload
        (when-let [conf (config (:url repository))]
          (doseq [[server channels] conf]
            (let [{:keys [com bot] :as com-map} (@bots server)
                  owner (-> repository :owner :name)
                  name (:name repository)
                  n-commits (count commits)
                  no-header (or (:no-header conf) (= n-commits 1))
                  branch (last (.split ref "/"))]
              (doseq [chan channels]
                (let [com-m (assoc com-map :channel chan)]
                  (when-not no-header
                    (send-message
                     com-m
                     (str "\u0002" owner "/" name "\u0002"
                          ": " (count commits) " new commit(s) on branch " branch
                          ". Compare view at <" (is-gd compare) ">. " (:open_issues repository)
                          " open issues remain."))))
                (doseq [commit (take 3 commits)]
                  (notify-chan com-m commit owner name branch no-header)))))))))
  (str
   "These boots are made for walkin' and that's just what they'll do. "
   "One of these days these boots are gonna walk all over you."))

(defplugin :irc
  (:init
   (fn [irc bot]
     (swap! bots assoc (:server @irc) {:irc irc :bot bot})))
  (:routes (POST "/commits" req (handler req))))