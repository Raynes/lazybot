(ns sexpbot.info
  (:use [clojure.contrib.duck-streams :only [spit]])
  (:import (java.io File)))

(def *info-file* (str (System/getProperty "user.home") "/.sexpbot/info.clj"))

(defn setup-info []
  (let [ifile (File. *info-file*)]
    (when-not (.exists ifile)
      (.createNewFile ifile)
      (spit *info-file* "{}"))))

(defn read-config []
  (->> *info-file* slurp read-string))

(defn write-config [new-info]
  (spit *info-file* (-> (read-config) (merge new-info))))

(defn get-key [key]
  (-> key ((read-config))))

(defn remove-key [key]
  (spit *info-file* (-> (read-config) (dissoc key))))

(defn set-key [key nval]
  (-> (read-config) (assoc key nval) write-config))

(defn format-config []
  (->> (read-config) str (#(.replaceAll % "," "\n")) (spit *info-file*)))

(defmacro with-info [file & body]
  `(binding [*info-file* ~file] ~@body))