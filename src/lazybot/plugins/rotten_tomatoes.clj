(ns lazybot.plugins.rotten-tomatoes
  (:use lazybot.registry)
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.string :as string]))

(def base "http://api.rottentomatoes.com/api/public/v1.0/")

(defn compose-message [movie]
  (when movie
    (let [{:strs [title year critics_consensus ratings links]} movie
          score (ratings "critics_score")
          link (links "alternate")]
      (format "%s -- %s | rating: %s | consensus: %s | link: %s"
              year title score critics_consensus link))))

(defn get-ratings [key query]
  (-> (http/get (str base "movies")
                {:query-params {:apikey key
                                :q query
                                :page_limit 1
                                :page 1}})
    :body
    json/parse-string
    (get "movies")
    first
    compose-message))

(defplugin
  (:cmd
    "Get the rotten tomatoes rating for a movie."
    #{"movie"}
    (fn [{:keys [bot args] :as com-m}]
      (send-message com-m
                    (or
                      (get-ratings
                        (get-in @bot [:config :rotten-tomatoes :key])
                        (string/join " " args))
                      "Movie not found.")))))
