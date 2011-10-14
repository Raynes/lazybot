(ns lazybot.plugins.log
  (:use (lazybot registry utilities)
        [lazybot.plugins.login :only [when-privs]]
        clojure.tools.logging)
  (:import [org.apache.log4j Level]))

(defn str->package [s]
  (if (not= (.indexOf s ".") -1)
    s
    (str "lazybot.plugins." s)))

(defn all-loaded-plugins [bot]
  (let [module-list (-> bot :modules keys)]
    (debug module-list)
    (map name module-list)))

(defn plugin-list [bot args]
  (map str->package
       (or (and (not= args ["all"])
                (seq args))
           (all-loaded-plugins bot))))

(defn set-log-level [package level]
  (info (str "Setting log level for " package " to " level))
  (.setLevel (get-logger package) level))

(defplugin
  (:cmd
   "Set log level for specified plugins. Example: log debug clojure ping -  specify \"all\" or an empty list to affect all plugins."
   #{"log"}
   (fn [{:keys [bot nick com channel args] :as com-m}]
     (let [[level & pkgs] args
           level (Level/toLevel (.toUpperCase level))]
       (when-privs
        com-m :admin
        (doseq [p (plugin-list @bot pkgs)]
          (set-log-level p level)))))))