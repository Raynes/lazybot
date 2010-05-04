(ns sexpbot.info
  (:import java.io.File
	   org.apache.commons.io.FileUtils))

(def sexpdir (File. (str (System/getProperty "user.home") "/.sexpbot" )))
(def info-file (str sexpdir "/info.clj"))

(when-not (.exists sexpdir) 
  (FileUtils/copyDirectory (File. (str (System/getProperty "user.dir") "/.sexpbot")) sexpdir))