(ns sexpbot.twitter
  (:use [clojure
         [string :only [join]] [set :only [difference]]
         [stacktrace :only [print-stack-trace]]]
        clojure.contrib.logging
        [clj-config.core :only [read-config]]
        [sexpbot
         core
         [info :only [info-file]]
         [registry :only [send-message prefix]]
         [utilities :only [keywordize on-thread]]
         [gist :only [trim-with-gist]]]
        [somnium.congomongo :only [insert! fetch-one]])
  (:require [oauth.client :as oauth]
            twitter))

(defn set-log-level! [level]
  "Sets the root logger's level, and the level of all of its Handlers, to level.
   Level should be one of the constants defined in java.util.logging.Level."
  (let [logger (.getLogger (impl-get-log ""))]
    (.setLevel logger level)
    (doseq [handler (.getHandlers logger)]
      (. handler setLevel level))))

(set-log-level! java.util.logging.Level/OFF)

(def twitter-info (:twitter initial-info))

(def consumer
     (let [{:keys [consumer-key consumer-secret]} twitter-info]
       (oauth/make-consumer consumer-key consumer-secret
                            "http://twitter.com/oauth/request_token"
                            "http://twitter.com/oauth/access_token"
                            "http://twitter.com/oauth/authorize"
                            :hmac-sha1)))

(defn get-mentions [{:keys [token token-secret]}]
  (try
    (twitter/with-oauth
      consumer token token-secret
      (set (twitter/mentions)))
    (catch Exception e
      (println "An error occured while trying to get mentions:")
      (print-stack-trace e)
      #{})))

(defn drop-name [s] (join " " (rest (.split s " "))))

(defn format-log [{{user :screen_name} :user text :text}]
  (str user ": " text))

(defn twitter-loop [_]
  (let [{:keys [token token-secret]} (fetch-one :twitter)
        com (ref {:token token :token-secret token-secret :consumer consumer
                  :server :twitter :name (:bot-name twitter-info)})
        bot (ref {:protocol :twitter
                  :modules {:internal {:hooks initial-hooks}}
                  :config initial-info
                  :pending-ops 0})]
    (on-thread
     (loop [stale-mentions (get-mentions @com)]
       (Thread/sleep (or (:interval twitter-info) 120000))
       (let [mentions (get-mentions @com)
             new-mentions (difference mentions stale-mentions)]
         (doseq [{raw-text :text :as mention} new-mentions]
           (let [text (StringEscapeUtils/unescapeHtml raw-text)
                 nick (-> mention :user :screen_name)]
             (println (str "Received tweet from " nick ": " text))
             (call-all {:bot bot :com com :nick nick
                        :channel nick :message (drop-name text)}
                       :on-message)))
         (recur mentions))))
    [com bot]))

(defmethod send-message :twitter
  [{:keys [com bot nick message]} s]
  (let [{:keys [token token-secret consumer name]} @com
        msg (trim-with-gist
              140
              "result.clj"
              (str "@" nick " " message "\n@" name ": \u27F9 ")
              (str "@" nick " " (.replace s "\n" "")))]
    (println "Sending tweet:" msg)
    (when-let [dupe (:id
                     (some
                      #(and (= msg (:text %)) %)
                      (twitter/user-timeline :screen-name name)))]
      (println "Duplicate tweet found. Destroying it.")
      (try
        (twitter/with-oauth consumer token token-secret
          (twitter/destroy-status dupe))
        (catch Exception e
          (println "An error occurred while trying to destroy a tweet:")
          (print-stack-trace e))))
    (try
      (twitter/with-oauth consumer token token-secret
        (twitter/update-status msg))
      (catch Exception e
        (println "An error occurred while trying to update your status:")
        (print-stack-trace e)))))

(defmethod prefix :twitter [bot nick & s] s)

(defn setup-twitter []
  (println "Hi! I'm sexpbot! Shall we set up twitter support? We shall!")
  (println "Have you set up a twitter application for sexpbot at"
           "http://twitter.com/oauth_clients/new, and configured sexpbot with"
           "Your consumer keys and such? y/n")
  (when (= (read-line) "y")
    (println "Alright. Fetching a request token...")
    (let [request-token (oauth/request-token consumer)]
      (println "Go to this url to approve the application:"
               (oauth/user-approval-uri consumer (:oauth_token request-token)))
      (println "Type in the PIN that twitter gives you and press enter.")
      (let [{token :oauth_token,
             token-secret :oauth_token_secret}
            (oauth/access-token consumer request-token (read-line))]
        (insert! :twitter (keywordize [token token-secret])))
      (println "All done! Have a nice day."))))

