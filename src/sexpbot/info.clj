(ns sexpbot.info
  (:use [clojure.contrib.io :only [spit]])
  (:import [java.io File BufferedReader FileReader]
	   org.apache.commons.io.FileUtils))

(def sexpdir (File. (str (System/getProperty "user.home") "/.sexpbot" )))

(when-not (.exists sexpdir) 
  (FileUtils/copyDirectory (File. (str (System/getProperty "user.dir") "/.sexpbot")) sexpdir))

(def *info-file* (str sexpdir "/info.clj"))

(defn format-config []
  (spit *info-file* 
	(apply str 
	       (reduce #(conj % 
			      (if (= \, (last %2)) 
				(apply str (butlast %2))
				%2)) 
		       [] 
		       (line-seq (BufferedReader. (FileReader. *info-file*)))))))

(defn setup-info []
  (let [ifile (File. *info-file*)]
    (when-not (.exists ifile)
      (.createNewFile ifile)
      (spit *info-file* "{}"))))

(defn read-config [& {flag :string? :or {flag false}}]
  (let [file (slurp *info-file*)]
    (if flag file (read-string file))))

(defn write-config [new-info]
  (spit *info-file* (-> (read-config) (merge new-info)))
  (format-config))

(defn get-key [key]
  (-> key ((read-config))))

(defn remove-key [key]
  (spit *info-file* (-> (read-config) (dissoc key)))
  (format-config))

(defn set-key [key nval]
  (-> (read-config) (assoc key nval) write-config))

(defmacro with-info [file & body]
  `(binding [*info-file* ~file] ~@body))