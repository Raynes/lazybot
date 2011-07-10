(ns sexpbot.plugins.javadoc
  (:use sexpbot.registry)
  (:require [clojure.string :as s]))

(def ^{:dynamic true}
  *javadoc-base-url* "http://download.oracle.com/javase/6/docs/api/")

(defn javadoc-url
  ([class]
     (str *javadoc-base-url*
          (s/replace (.getName class) "." "/")))
  ([class member]
     (javadoc-url class) ; would love to get this working
     ))

(defplugin :irc
  (:cmd
   "Find out what the most users ever in this channel at any one time is."
   #{"javadoc"}
   (fn [{channel :channel [class member] :args :as com-m}]
     (send-message com-m
                   (let [c (resolve (symbol class))]
                     (if (instance? Class c)
                       (javadoc-url c member)
                       (str "Javadoc not found. Try " *javadoc-base-url*)))))))