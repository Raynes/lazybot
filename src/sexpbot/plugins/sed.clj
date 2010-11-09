(ns sexpbot.plugins.sed
  (:use [sexpbot respond info]
        clj-config.core
        [clojure.string :only [join]]))

(def message-map (atom {}))
(def sed-regex #"^s/([^/]+)/([^/]*)/?")

(defn- format-msg [irc bot nick channel]
  (send-message irc bot channel (str nick ": Format is sed [-<user name>] s/<regexp>/<replacement>/ Try <prefix>help sed")))

(defn sed* [string regexp replacement]
  (try
    (.replaceAll string (str "(?i)" regexp) replacement)))

(defn sed [irc bot channel nick args verbose?]
  (let [arg-str (.trim (join " " args))
        try-to-match (fn [regex]
                       (or (second (re-find regex arg-str))
                           ""))
        user-to (try-to-match #"^\s*-(\w+)")
        margs (try-to-match #"\s*(s/[^/]+/[^/]*/?)$")
        orig-msg (some #(get (get-in @message-map [irc channel])
                             %)
                       [user-to :channel-last])
        [regexp replacement] (next (re-find sed-regex margs))]
    (cond
     (and verbose? (empty? orig-msg))
     (send-message irc bot channel "No one said anything yet!")

     (and verbose? (not-any? seq [regexp replacement]))
     (format-msg irc bot nick channel)

     :else
     (try
       (let [new-msg (sed* orig-msg regexp replacement)]
         (when-not (= orig-msg new-msg)
           (send-message irc bot channel (str "<" user-to "> " new-msg))))
       (catch Exception _
         (when verbose? (format-msg irc bot nick channel)))))))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [irc bot nick message channel]}]
     (when (seq (re-find sed-regex message))
       (sed irc bot channel nick [(str "-" nick) message] false))
     (when (and (not= nick (:name @irc))
                (not= (take 4 message)
                      (-> @bot :config :prepends first (str "sed"))))
       (swap! message-map update-in [irc channel]
              assoc nick message, :channel-last message))))

  (:cmd
   "Simple find and replace. Usage: sed [-<user name>] s/<regexp>/<replacement>/
If the specified user isn't found, it will default to the last thing said in the channel. 
Example Usage: sed -boredomist s/[aeiou]/#/
Shorthand : s/[aeiou]/#/"
   #{"sed"}
   (fn [{:keys [irc bot channel args nick]}]
     (sed irc bot channel nick args true))))
