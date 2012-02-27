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
      (format "%s - %s | rating: %s | consensus: %s | link: %s"
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

(defn rating [movie]
  (let [rating (get-in movie ["ratings" "critics_score"])]
    (when rating
      (Integer. rating))))

(defn in-theaters [key]
  (-> (http/get (str base "lists/movies/in_theaters.json")
                {:query-params {:apikey key}})
    :body
    json/parse-string
    (get "movies")
    (->>
      (sort-by rating)
      reverse
      (map compose-message))))

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
                      "Movie not found."))))
  
  (:cmd
    "Get a list of the top 5 highest rated movies currently in theaters."
    #{"playing"}
    (fn [{:keys [bot nick args] :as com-m}]
      (doseq [message (take (or (when (seq args) (Integer. (first args))) 5)
                            (in-theaters
                              (get-in @bot [:config :rotten-tomatoes :key])))]
        (send-message (assoc com-m :channel nick) message :notice? true)))))
