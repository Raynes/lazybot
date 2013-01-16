(ns lazybot.plugins.google
  (:require [lazybot.registry :refer [defplugin send-message]]
            [lazybot.utilities :refer [trim-string]]
            [cheshire.core :refer [parse-string]] 
            [clojure.string :as s]
            [clj-http.client :as http])
  (:import org.apache.commons.lang.StringEscapeUtils
           java.net.URLDecoder))

(defn google [term]
  (-> (http/get "http://ajax.googleapis.com/ajax/services/search/web"
                {:query-params {"v" "1.0" "q" term}})
      :body
      parse-string))

(defn handle-search [{:keys [args] :as com-m}]
  (send-message com-m
                (let [argstr (s/join " " args)]
                  (if-not (seq (s/trim argstr))
                    (str "No search term!")
                    (let [results (google argstr)
                          {:strs [url titleNoFormatting]} (first (get-in results ["responseData" "results"]))
                          res-count (get-in results ["responseData" "cursor" "estimatedResultCount"])]
                      (if (and results url)
                        (str "["
                             (trim-string 80 (constantly "...")
                                          (StringEscapeUtils/unescapeHtml titleNoFormatting))
                             "] "
                             (URLDecoder/decode url "UTF-8"))))))))

(defplugin
  (:cmd
   "Searches google for whatever you ask it to, and displays the first result and the estimated
   number of results found."
   #{"google"}
   #'handle-search)

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