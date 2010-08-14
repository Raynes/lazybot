(ns sexpbot.plugins.git
  (:use [sexpbot respond]
	clojure.java.shell)
  )

(defn git-pull []
  (let [out (sh "git" "pull")]
    (if-not (zero? (out :exit)) ; an error occurred
      (out :err)
      (out :out))))

(defplugin
  (:gitpull
   "Updates sexpbot from git (must be admin)"
   ["gitpull"]
   [{:keys [irc channel nick args] :as ircm}]
   (if-admin nick ircm
     (send-message irc channel (git-pull)))))