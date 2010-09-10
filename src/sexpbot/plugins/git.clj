(ns sexpbot.plugins.git
  (:use [sexpbot respond]
	clojure.java.shell))

(defn git-pull []
  (let [out (sh "git" "pull")]
    (if-not (zero? (out :exit)) ; an error occurred
      (out :err)
      (out :out))))

(defplugin
  (:cmd
   "Updates sexpbot from git (must be admin)"
   #{"gitpull"}
   (fn [{:keys [irc bot channel nick args] :as ircm}]
     (if-admin nick ircm bot
               (send-message irc bot channel (git-pull))))))