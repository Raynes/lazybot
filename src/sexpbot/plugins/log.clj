(ns sexpbot.plugins.log
  (:use [sexpbot respond info]
	[clojure.contrib.io :only [writer]])
  (:import (java.io File FileOutputStream)))

(defn create-log-file [chan bot irc]
  (let [logdir (File. (str sexpdir "/logs"))
	file (File. (str logdir "/" (:server @irc) "-" chan ".ilog"))]
    (when-not (.exists logdir)
      (.mkdir logdir))
    (when-not (.exists file)
      (.createNewFile file))
    (dosync (alter bot assoc-in [:log-writers chan] (writer (FileOutputStream. file true))))))

(defn write-log [{:keys [raw-message bot channel irc]}]
  (when (:log (:config @bot)))
  (when-not (and (:log-writers @bot) ((:log-writers @bot) channel))
    (if channel
      (create-log-file channel bot irc)
      (create-log-file (:server @irc) bot irc)))
  (binding [*out* ((:log-writers @bot) (if channel channel (:server @irc)))]
    (println raw-message)
    (flush)))

(defplugin
  (:hook :on-any write-log))