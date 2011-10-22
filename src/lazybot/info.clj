(ns lazybot.info
  (:import java.io.File
	   org.apache.commons.io.FileUtils))

(def sexpdir (File. (str (System/getProperty "user.home") "/.lazybot" )))
(def info-file (str sexpdir "/config.clj"))

(when-not (.exists sexpdir) 
  (FileUtils/copyDirectory (File. (str (System/getProperty "user.dir") "/.lazybot")) sexpdir))