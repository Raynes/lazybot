(ns lazybot.buildbot.xmlutil
  (:require [clojure.xml :as c-xml]
            [clojure.data.xml :as c-d-xml]
            [clojure.zip :as c-zip]
            [clojure.data.zip :as c-d-zip]
            [clojure.data.zip.xml :as c-d-z-xml :refer [xml-> xml1-> attr= text]]
            [clojure.pprint :refer [pprint]]))

(defn dbg [node]
  (if (associative? node)
    (c-xml/emit-element (dissoc node :content))
    (c-xml/emit-element node))
  node)

(defn as-short-xml [node]
  (clojure.string/trim ; remove trailing \n
    (with-out-str
      (if (associative? node)
        (c-xml/emit-element (dissoc node :content))
        (c-xml/emit-element node)))))

(defn dz [zipper] (do
                    (dbg (clojure.zip/node zipper))
                    zipper)) ; return the zipper for more processing
(defn az [zipper] (as-short-xml (clojure.zip/node zipper)))