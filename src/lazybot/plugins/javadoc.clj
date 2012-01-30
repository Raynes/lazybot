(ns lazybot.plugins.javadoc
  (:use lazybot.registry)
  (:require [clojure.string :as s]))

(def ^{:dynamic true}
  *javadoc-base-url* "http://docs.oracle.com/javase/6/docs/api/")

(defn format-varargs [class]
  (if-not (.isArray class)
    (.getName class)
    (str (.getName (.getComponentType class)) "...")))

(defn arglist-anchor-components [method]
  (let [args (.getGenericParameterTypes method)]
    (if-not (.isVarArgs method)
      (map #(.getName %) args)
      (let [[easy hard] ((juxt butlast last) args)]
        (concat (map #(.getName %) easy)
                [(format-varargs hard)])))))

(defn matching-members [class name]
  (->> (.getMethods class)
       (filter #(= name (.getName %)))))

(defn javadoc-url
  ([class]
     (str *javadoc-base-url*
          (s/replace (.getName class) "." "/")
          ".html"))
  ([class member]
     (let [base (javadoc-url class)
           guessed-target (first (matching-members class member))]
       (str base (when guessed-target
                   (str "#" member "("
                        (s/join ",%20"
                                (arglist-anchor-components guessed-target))
                        ")"))))))

(defplugin
  (:cmd
   "Lookup Oracle's javadocs for a class. Input can be a fully qualified class name,
   a class name and a method/field, or a non-qualified class name."
   #{"javadoc"}
   (fn [{channel :channel [class member] :args :as com-m}]
     (send-message com-m
                   (let [c (resolve (symbol class))]
                     (if (instance? Class c)
                       (javadoc-url c member)
                       (str "Javadoc not found. Try " *javadoc-base-url*)))))))
