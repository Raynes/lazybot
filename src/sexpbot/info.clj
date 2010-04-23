(ns sexpbot.info
  (:use [clojure.contrib.io :only [spit slurp*]])
  (:import [java.io File BufferedReader FileReader]
	   org.apache.commons.io.FileUtils))

(def sexpdir (File. (str (System/getProperty "user.home") "/.sexpbot" )))

(when-not (.exists sexpdir) 
  (FileUtils/copyDirectory (File. (str (System/getProperty "user.dir") "/.sexpbot")) sexpdir))

(def *info-file* (str sexpdir "/info.clj"))

(defn format-config [newc]
  (.replaceAll newc ",s*(?=([^\"]*\"[^\"]*\")*[^\"]*$)"  "\n"))

(defn setup-info []
  (let [ifile (File. *info-file*)]
    (when-not (.exists ifile)
      (.createNewFile ifile)
      (spit *info-file* "{}"))))

(defn read-config [& {:keys [string?] :or {string? false}}]
  (let [file (slurp *info-file*)]
    (if string? file (read-string file))))

(defn write-config [new-info]
  (spit *info-file* (-> (read-config) (merge new-info) str format-config)))

(defn get-key [key]
  (-> key ((read-config))))

(defn remove-key [key]
  (spit *info-file* (-> (read-config) (dissoc key) str format-config)))

(defn set-key [key nval]
  (-> (read-config) (assoc key nval) write-config))

(defmacro with-info [file & body]
  `(binding [*info-file* ~file] ~@body))