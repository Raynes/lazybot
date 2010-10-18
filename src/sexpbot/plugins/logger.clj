(ns sexpbot.plugins.logger
  (:use [sexpbot respond]
        [clj-time.core :only [now]]
        [clj-time.format :only [unparse formatters]]
        [clojure.java.io :only [file]])
  (:import [java.io File]))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [irc bot nick channel message]}]
     (let [config (:config @bot)
           server (:server @irc)
           last-channel (apply str (remove #(= % \#) channel))]
       (if (get-in config [server :log channel])
         (let [log-dir  (file (:log-dir (config server)) server last-channel)
               date     (unparse (formatters :date) (now))
               time     (unparse (formatters :time) (now))
               log-file (file log-dir (str date ".txt"))]
           (.mkdirs log-dir)
           (spit log-file
                 (format "[%s]: %s: %s\n" time nick message)
                 :append true)))))))