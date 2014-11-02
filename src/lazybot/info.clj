(ns lazybot.info
  (:require [clj-config.core :as cfg]
            [clojure.java.io :refer [file]])
  (:import java.io.File
	   org.apache.commons.io.FileUtils))

(def ^:dynamic *lazybot-dir* (file (System/getProperty "user.home") ".lazybot"))

(defn read-config []
  (when-not (.exists *lazybot-dir*) 
    (FileUtils/copyDirectory
     (File. (str (System/getProperty "user.dir")
                 "/.lazybot"))
     *lazybot-dir*))
  (eval (cfg/read-config (file *lazybot-dir* "config.clj"))))
