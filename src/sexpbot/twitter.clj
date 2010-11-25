(ns sexpbot.twitter
  (:use [clojure [string :only [join]] [set :only [difference]]]
        [clj-config.core :only [read-config]]
        [sexpbot
         core
         [info :only [info-file]]
         [registry :only [send-message]]
         [utilities :only [keywordize on-thread]]]
        [somnium.congomongo :only [insert! fetch-one]])
  (:require [oauth.client :as oauth]
            twitter))

(def twitter-info (:twitter initial-info))

(def consumer
     (let [{:keys [consumer-key consumer-secret]} twitter-info]
       (oauth/make-consumer consumer-key consumer-secret
                            "http://twitter.com/oauth/request_token"
                            "http://twitter.com/oauth/access_token"
                            "http://twitter.com/oauth/authorize"
                            :hmac-sha1)))

(defn get-mentions [{:keys [token token-secret]}]
  (twitter/with-oauth
    consumer token token-secret
    (set (twitter/mentions))))

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
         (doseq [{text :text :as mention} new-mentions]
           (println "Received tweet:" text)
           (call-all {:bot bot
                      :com com
                      :nick (-> mention :user :screen_name)
                      :message (drop-name text)}
                     :on-message))
         (recur mentions))))
    [com bot]))

(defmethod send-message :twitter
  [{:keys [com bot nick]} s]
  (let [{:keys [token token-secret consumer]} @com
        msg (str "@" nick " " s)]
    (println "Sending tweet:" msg)
    (when-let [dupe (:id
                     (some
                      #(and (= msg (:text %)) %)
                      (twitter/user-timeline :screen-name (:name @com))))]
      (println "Duplicate tweet found. Destroying it.")
      (twitter/with-oauth consumer token token-secret
        (twitter/destroy-status dupe)))
    (twitter/with-oauth consumer token token-secret
      (twitter/update-status msg))))

(defn setup-twitter []
  (println "Hi! I'm sexpbot! Shall we set up twitter support? We shall!")
  (println "Have you set up a twitter application for sexpbot at"
           "http://twitter.com/oauth_clients/new yet? y/n")
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

