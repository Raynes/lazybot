(ns lazybot.plugins.logger
  (:use [lazybot registry]
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

(defn log-message [{:keys [com bot nick channel message action?]}]
  (let [config (:config @bot)
        server (:server @com)
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

(defplugin :irc
  (:hook :on-message #'log-message)
  (:hook
   :on-send-message
   (fn [com bot channel message action?]
     (log-message {:com com :bot bot :channel channel :message message
                   :nick (:name @com) :action? action?})
     message)))