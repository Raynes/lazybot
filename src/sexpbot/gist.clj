(ns sexpbot.gist
  (:require [com.twinql.clojure.http :as http]
	    [org.danlarkin.json :as json])
  (:import (java.net URI)))

(def posturl "http://gist.github.com/api/v1/json/new")

(defn cull [js] (-> js json/decode-from-str :gists first :repo))

(defn post-gist [fname contents]
  (->> (http/post posturl :query {(str "files[" fname "]") contents} :as :string) 
       :content 
       cull
       (str "http://gist.github.com/")))