(ns lazybot.plugins.google
  (:require [lazybot.registry :refer [defplugin send-message]]
            [lazybot.utilities :refer [trim-string]]
            [cheshire.core :refer [parse-string]] 
            [clojure.string :as s]
            [clj-http.client :as http])
  (:import org.apache.commons.lang.StringEscapeUtils
           java.net.URLDecoder))

(defn google [service term]
  "Services: \"web\", \"images\""
  (-> (http/get (str "http://ajax.googleapis.com/ajax/services/search/"
                     service)
                {:query-params {"v"  "1.0"
                                "rsz" 8 ; 8 results
                                "q"   term}})
      :body
      parse-string))

(defn search-string
  "From an argument list, builds a search string."
  [args]
  (->> args
       (s/join " ")
       s/trim))

(defn handle-search [com-m]
  (send-message com-m
                (let [q (search-string (:args com-m))]
                  (if-not (seq q)
                    (str "No search terms!")
                    (let [results (google "web" q)
                          {:strs [url titleNoFormatting]}
                            (first (get-in results ["responseData" "results"]))
                          res-count (get-in results ["responseData"
                                                     "cursor"
                                                     "estimatedResultCount"])]
                      (if (and results url)
                        (str "["
                             (trim-string 80 (constantly "...")
                                          (StringEscapeUtils/unescapeHtml
                                            titleNoFormatting))
                             "] "
                             (URLDecoder/decode url "UTF-8"))))))))

(defn handle-image-search
  "Finds a random google image for a string, and responds with the URI."
  [com-m]
  (send-message com-m
                (let [q (search-string (:args com-m))]
                  (if-not (seq q)
                    (str "No search terms!")
                    (-> (google "images" q)
                        (get "responseData")
                        (get "results")
                        rand-nth
                        (get "url")
                        (URLDecoder/decode "UTF-8"))))))

(defplugin
  (:cmd
   "Searches google for whatever you ask it to, and displays the first result
   and the estimated number of results found."
   #{"google"}
   #'handle-search)

  (:cmd
    "Searches google for a string, and returns a random image from the results."
    #{"gis"}
    #'handle-image-search)

  (:cmd
   "Searches wikipedia via google."
   #{"wiki"}
   (fn [args]
     (handle-search (assoc args :args (conj (:args args) "site:en.wikipedia.org")))))

  (:cmd
   "Searches encyclopediadramtica via google."
   #{"ed"}
   (fn [args]
     (handle-search (assoc args :args (conj (:args args) "site:encyclopediadramatica.com"))))))
