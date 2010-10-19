(ns sexpbot.plugins.logger
  (:use [sexpbot respond]
        [clj-time.core :only [now]]
        [clj-time.format :only [unparse formatters]]
        [clojure.java.io :only [file]])
  (:import [java.io File]))

(defn log-message [{:keys [irc bot nick channel message action?]}]
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
              (if action?
                (format "[%s]: *%s %s\n" time nick message)
                (format "[%s]: %s: %s\n" time nick message))
              :append true)))))

(defplugin
  (:hook
   :on-message
   (fn [irc]
     (log-message irc)))
  (:hook
   :on-send-message
   (fn [irc bot channel message action?]
     (log-message {:irc irc :bot bot :channel channel :message message
                   :nick (:name @irc) :action? action?})
     message)))