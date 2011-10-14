(ns lazybot.plugins.google
  (:use [lazybot registry [utilities :only [trim-string]]]
        [clojure-http.client :only [add-query-params]]
        [clojure.data.json :only [read-json]])
  (:require [clojure-http.resourcefully :as res]
            [clojure.string :as s])
  (:import org.apache.commons.lang.StringEscapeUtils
           java.net.URLDecoder))

(defn google [term]
  (-> (res/get (add-query-params "http://ajax.googleapis.com/ajax/services/search/web"
                                 {"v" "1.0" "q" term}))
      :body-seq first read-json))

(defn cull [result-set]
  [(:estimatedResultCount (:cursor (:responseData result-set)))
   (first (:results (:responseData result-set)))])

(defn handle-search [{:keys [args] :as com-m}]
  (send-message com-m
                (let [argstr (s/join " " args)]
                  (if-not (seq (.trim argstr))
                    (str "No search term!")
                    (let [[res-count {title :titleNoFormatting
                                      url :url}] (-> argstr google cull)]
                      (str "["
                           (trim-string 80 (constantly "...")
                                        (StringEscapeUtils/unescapeHtml title))
                           "] "
                           (URLDecoder/decode url "UTF-8")))))))

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