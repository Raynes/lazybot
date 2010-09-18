(ns sexpbot.plugins.github
  (:use sexpbot.respond
        [sexpbot.plugins.shorturl :only [is-gd]]
        [compojure.core :only [POST]]
        clojure.contrib.json)
  (:import java.net.InetAddress))

(def bots (atom {}))

(extend nil Read-JSON-From {:read-json-from (fn [& _] nil)})

(defn grab-config [] (-> @bots vals first :bot deref :config))

(defn filter-false-str [s]
  (apply str (filter identity s)))

(defn format-vec [v]
  (filter-false-str ["[" (apply str (interpose ", " (take 5 v)))
                     (when (> (count v) 5) "...") "]"]))

(defn notify-chan [irc bot chan commit]
  (send-message
   irc bot chan
   (filter-false-str ["\u0002" (-> commit :author :name) "\u0002: "
                      (when-let [added (seq (:added commit))]
                        (str "\u0002Added:\u0002 " (format-vec added) ". "))
                      (when-let [modified (seq (:modified commit))]
                        (str "\u0002Modified:\u0002 " (format-vec modified) ". "))
                      (when-let [removed (seq (:removed commit))]
                        (str "\u0002Removed:\u0002 " (format-vec removed) ". "))
                      "\u0002With message:\u0002 " (:message commit)])))

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
            (let [{:keys [irc bot]} (@bots server)
                  owner (-> repository :owner :name)
                  name (:name repository)]
              (doseq [chan channels]
                (send-message
                 irc bot chan
                 (str "\u0002" owner "/" name "\u0002"
                      ": " (count commits) " new commit(s) on branch " (last (.split ref "/"))
                      ". Compare view at <" (is-gd compare) ">. " (:open_issues repository)
                      " open issues remain."))
                (doseq [commit (take 3 commits)]
                  (notify-chan irc bot chan commit)))))))))
  "These boots are made for walkin' and that's just what they'll do.")

(defplugin
  (:init
   (fn [irc bot]
     (swap! bots assoc (:server @irc) {:irc irc :bot bot})))
  (:routes (POST "/" req (handler req))))