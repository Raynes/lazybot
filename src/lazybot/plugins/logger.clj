(ns lazybot.plugins.logger
  (:use [lazybot registry]
        [clj-time.core :only [now from-time-zone time-zone-for-offset]]
        [clj-time.format :only [unparse formatters]]
        [clojure.java.io :only [file]]
        [clojure.string :only [join]]
        [compojure.core :only [routes]]
        [hiccup.page :only [html5]])
  (:require [compojure.core :refer [GET]]
            [clj-http.util])
  (:import [java.io File]))

(defn config
  "Returns the current config."
  []
  (-> lazybot.core/bots
    deref
    first
    val
    :bot
    deref
    :config))

(defn servers
  "Returns a list of all servers with logging enabled."
  ([] (servers (config)))
  ([config]
   (map key (filter (fn [[server config]]
                      (and (string? server)
                           (some #{"logger"} (:plugins config))))
                    config))))

(defn channels
  "Returns a list of all channels on a given server."
  ([server] (channels (config) server))
  ([config server]
   (get-in config [server :log])))

(defn log-dir
  "The log directory for a particular server and channel, if one exists."
  ([server channel] (log-dir (config) server channel))
  ([config server channel]
   (let [short-channel (apply str (remove #(= % \#) channel))]
     (when (get-in config [server :log channel])
       (file (:log-dir (config server)) server short-channel)))))

(defn log-files
  "A list of log files for a server and channel."
  [server channel]
  (when-let [dir (log-dir server channel)]
    (filter #(re-matches #".+\.txt" (.getName %))
            (.listFiles dir))))

(defn date-time [opts]
  ;; What? Why doesn't clj-time let you unparse times in a timezone other than GMT?
  (let [offset (or (:time-zone-offset opts) -6)
        time   (from-time-zone (now) (time-zone-for-offset (- offset)))]
    [(unparse (formatters :date) time)
     (unparse (formatters :hour-minute-second) time)]))

(defn log-message [{:keys [com bot nick channel message action?]}]
  (let [config (:config @bot)
        server (:server @com)]
    (when-let [log-dir (log-dir config server channel)]
      (let [[date time] (date-time config)
            log-file (file log-dir (str date ".txt"))]
        (.mkdirs log-dir)
        (spit log-file
              (if action?
                (format "[%s] *%s %s\n" time nick message)
                (format "[%s] %s: %s\n" time nick message))
              :append true)))))

(defn link
  "Link to a logger URI."
  [name & parts]
  (let [uri (join "/" (cons "/logger"
                            (map clj-http.util/url-encode parts)))]
    [:a {:href uri} name]))

(def error-404
  {:status 404
   :headers {}
   :body "These are not the logs you're looking for."})

(defn layout
  "Takes a hiccup document, wraps it with the layout, and renders the resulting
  HTML to a string. Passes through hashmaps directly."
  [title content]
  (if (map? content)
    content
    (html5
      [:head
       [:title title]]
      [:body content])))

(defn file-index
  "A Ring response for a specific log file."
  [server channel file]
  (let [file (first (filter #(= file (.getName %))
                            (log-files server channel)))]
    (when file
      {:status 200
       :headers {"Content-Type" "text/plain; charset=UTF-8"}
       :body file})))

(defn channel-index
  "A hiccup doc describing logs on a server and channel."
  [server channel]
  (when (log-dir server channel)
    (let [logs (map #(.getName %) (log-files server channel))]
      (list
        [:h1 "Logs for " channel " on " server]
        [:ol
         (map (fn [log]
                [:li (link log server channel log)])
              logs)]))))

(defn server-index
  "A hiccup doc describing logs on a server."
  [server]
  (when (some #{server} (servers))
    (list
      [:h2 "Channels on " server]
      [:ul
       (map (fn [channel]
              [:li (link channel server channel)])
            (channels server))])))

(defn index
  "Renders an HTTP index of available logs."
  [req]
  (layout "IRC Logs"
          (cons [:h1 "All channel logs"]
                (mapcat server-index (servers)))))

(def pathreg #"[^\/]+")

(defplugin
  (:routes (routes
             (GET "/logger" req (index req))
             (GET ["/logger/:server" :server pathreg] [server]
                  (layout server (server-index server)))
             (GET ["/logger/:server/:channel"
                   :server pathreg
                   :channel pathreg]
                  [server channel]
                  (layout (str server channel)
                          (channel-index server channel)))
             (GET ["/logger/:server/:channel/:file"
                   :server pathreg
                   :channel pathreg
                   :file pathreg]
                  [server channel file]
                  (file-index server channel file))
             (constantly error-404)))
  (:hook :on-message #'log-message)
  (:hook
    :on-send-message
    (fn [com bot channel message action?]
      (log-message {:com com :bot bot :channel channel :message message
                    :nick (:name @com) :action? action?})
     message)))
