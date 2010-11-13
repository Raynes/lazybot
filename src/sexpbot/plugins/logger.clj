(ns sexpbot.plugins.logger
  (:use [sexpbot plugin]
        [clj-time.core :only [now from-time-zone time-zone-for-offset]]
        [clj-time.format :only [unparse formatters]]
        [clojure.java.io :only [file]])
  (:import [java.io File]))

(defn date-time [opts]
  ;; What? Why doesn't clj-time let you unparse times in a timezone other than GMT?
  (let [offset (or (:time-zone-offset opts) -6)
        time   (from-time-zone (now) (time-zone-for-offset (- offset)))]
    [(unparse (formatters :date) time)
     (unparse (formatters :hour-minute-second) time)]))

(defn log-message [{:keys [irc bot nick channel message action?]}]
  (let [config (:config @bot)
        server (:server @irc)
        last-channel (apply str (remove #(= % \#) channel))]
    (if (get-in config [server :log channel])
      (let [[date time] (date-time config)
            log-dir  (file (:log-dir (config server)) server last-channel)
            log-file (file log-dir (str date ".txt"))]
        (.mkdirs log-dir)
        (spit log-file
              (if action?
                (format "[%s] *%s %s\n" time nick message)
                (format "[%s] %s: %s\n" time nick message))
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