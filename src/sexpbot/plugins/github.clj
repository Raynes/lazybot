(ns sexpbot.plugins.github
  (:use sexpbot.respond
        [sexpbot.plugins.shorturl :only [is-gd]]
        ring.adapter.jetty
        ring.middleware.params
        net.cgrand.moustache
        clojure.contrib.json))

(def bots (atom {}))

(extend nil Read-JSON-From {:read-json-from (fn [& _] nil)})

(defn grab-config [] (-> @bots vals first :bot deref :config))

(defn compare-view [owner repo before after]
  (is-gd (str "http://github.com/" owner "/" repo "/compare/" before "..." after)))

(defn format-vec [v]
  (apply str
         (filter identity ["[" (apply str (interpose ", " (take 5 v)))
                           (when (> (count v) 5) "...") "]"])))

(defn notify-chan [irc bot chan commit]
  (send-message
   irc bot chan
   (apply str
          (filter identity ["\u0002" (-> commit :author :name) "\u0002: "
                            (when-let [added (seq (:added commit))]
                              (str "\u0002Added:\u0002 " (format-vec added) ". "))
                            (when-let [modified (seq (:modified commit))]
                              (str "\u0002Modified:\u0002 " (format-vec modified) ". "))
                            (when-let [removed (seq (:removed commit))]
                              (str "\u0002Removed:\u0002 " (format-vec removed) ". "))
                            "\u0002With message:\u0002 " (:message commit)]))))

(defn handler [req]
  (let [{:keys [before repository commits after] :as payload}
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
                    ": " (count commits) " new commit(s). Compare view at <"
                    (compare-view owner name before after) ">."))
              (doseq [commit (take 3 commits)]
                (notify-chan irc bot chan commit))))))))
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "These boots are made for walkin' and that's just what they'll do."})

(def routes (app wrap-params :post handler))

(def server (run-jetty #'routes {:port 8080 :join? false}))

(defplugin
  (:init
   (fn [irc bot]
     (swap! bots assoc (:server @irc) {:irc irc :bot bot})))
  (:cleanup #(.stop server)))