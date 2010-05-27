(ns sexpbot.plugins.log
  (:use [sexpbot info respond]
	clj-config.core
	[clojure.contrib.io :only [writer]])
  (:import (java.io File FileOutputStream)))

(defn create-log-file [chan irc]
  (let [logdir (File. (str sexpdir "/logs"))
	file (File. (str logdir "/" (:server @irc) "-" chan ".ilog"))]
    (when-not (.exists logdir)
      (.mkdir logdir))
    (when-not (.exists file)
      (.createNewFile file))
    (dosync (alter irc assoc-in [:log-writers chan] (writer (FileOutputStream. file true))))))

(defn write-log [{:keys [raw-message channel irc]}]
  (when (:log (read-config info-file)))
  (when-not (and (:log-writers @irc) ((:log-writers @irc) channel))
    (if channel
      (create-log-file channel irc)
      (create-log-file (:server @irc) irc)))
  (binding [*out* ((:log-writers @irc) (if channel channel (:server @irc)))]
    (println raw-message)
    (flush)))

(defplugin
  (:add-hook :on-any write-log))