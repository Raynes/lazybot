(ns sexpbot.twitter
  (:use [clojure.set :only [difference]]
        [clj-config.core :only [read-config]]
        [sexpbot core [info :only [info-file]] [registry :only [send-message]]]
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

(defn get-mentions [token token-secret]
  (twitter/with-oauth
    consumer token token-secret
    (into #{} (twitter/mentions))))

(defn drop-name [s] (apply str (interpose " " (rest (.split s " ")))))

(defn format-log [{{user :screen_name} :user text :text}]
  (str user ": " text))

(defn twitter-loop [_]
  (let [{:keys [token token-secret]} (fetch-one :twitter)
        com (ref {:token token :token-secret token-secret :consumer consumer
                  :server :twitter :name (:bot-name twitter-info)})
        bot (ref {:protocol "twitter"
                  :modules {:internal {:hooks initial-hooks}}
                  :config initial-info
                  :pending-ops 0})
        stale-mentions (atom (get-mentions (:token @com) (:token-secret @com)))]
    (.start
     (Thread.
      (fn []
        (while true
          (Thread/sleep 120000)
          (let [mentions (difference (get-mentions (:token @com) (:token-secret @com)) @stale-mentions)]
            (doseq [mention mentions]
              (swap! stale-mentions conj mention)
              (println "Received tweet:" (:text mention))
              (call-all {:bot bot
                         :com com
                         :nick (-> mention :user :screen_name)
                         :message (drop-name (:text mention))}
                        :on-message)))))))
    [com bot]))

(defmethod send-message "twitter"
  [{:keys [com bot nick]} s]
  (let [{:keys [token token-secret consumer]} @com
        msg (str "@" nick " " s)]
    (println "Sending tweet:" msg)
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
      (let [{:keys [oauth_token oauth_token_secret]}
            (oauth/access-token consumer request-token (read-line))]
        (insert! :twitter {:token oauth_token :token-secret oauth_token_secret}))
      (println "All done! Have a nice day."))))

