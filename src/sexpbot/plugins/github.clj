(ns sexpbot.plugins.github
  (:use sexpbot.respond
        [sexpbot.plugins.shorturl :only [is-gd]]
        [compojure.core :only [POST]]
        [clj-github.gists :only [new-gist]]
        clojure.contrib.json)
  (:require [clojure.contrib.string :as s])
  (:import java.net.InetAddress))

(def bots (atom {}))

(defn word-wrap [str]
  (s/replace-re #"(.{50,70}[])}\"]*)\s+" "$1\n" str))

(defn split-str-at [len s]
  (map #(apply str %)
       ((juxt take drop) len s)))

(def gist-note "... ")
(def default-cap 300)

(defn trim-with-gist
  "Trims the input string to a maximum of cap characters; if any
trimming is done, then a gist will be created to hold the entire
string, and the returned string will end with a link to that
gist. Prepends gist-prefix to the gist (if applicable), but not to the
result string - use this to give the gist additional context that is
not necessary in the result."
  ([s]
     (trim-with-gist default-cap "result.clj" "" s))
  ([opt s]
     (if (number? opt)
       (trim-with-gist opt "result.clj" "" s)
       (trim-with-gist default-cap opt "" s)))
  ([cap name s]
     (trim-with-gist cap name "" s))
  ([cap name gist-prefix s]
     (let [[before after] (split-str-at cap s)]
       (if-not (seq after)
         before
         (let [url (try
                     (str "http://gist.github.com/"
                          (:repo (new-gist {} name
                                           (word-wrap (str gist-prefix s)))))
                     (catch java.io.IOException _ "failed to gist"))]
           (str (s/take
                 (- cap (count gist-note) (count url)) s)
                gist-note url))))))

(extend nil Read-JSON-From {:read-json-from (constantly nil)})

(defn grab-config [] (-> @bots vals first :bot deref :config))

(defn format-vec [v]
  (let [[show hide] (split-at 3 v)]
    (s/join "" ["[" (s/join ", " show)
              (when (seq hide) "...") "]"])))

(defn notify-chan [irc bot chan commit]
  (send-message
   irc bot chan
   (s/join "" ["\u0002" (-> commit :author :name) "\u0002: "
             (when-let [added (seq (:added commit))]
               (str "\u0002Added:\u0002 " (format-vec added) ". "))
             (when-let [modified (seq (:modified commit))]
               (str "\u0002Modified:\u0002 " (format-vec modified) ". "))
             (when-let [removed (seq (:removed commit))]
               (str "\u0002Removed:\u0002 " (format-vec removed) ". "))
             "\u0002With message:\u0002 " (:message commit)])))

(defn handler [req]
  (let [remote (:remote-addr req)]
    (when (or (= "127.0.0.1" remote)
              (.endsWith (.getCanonicalHostName (InetAddress/getByName remote)) "github.com")))
    (let [{:keys [before repository commits after compare ref] :as payload}
          (read-json ((:form-params req) "payload"))
          config (:commits (grab-config))]
      (when payload
        (when-let [conf (config (:url repository))]
          (doseq [[server channels] conf]
            (let [{:keys [irc bot]} (@bots server)
                  owner (-> repository :owner :name)
                  name (:name repository)]
              (doseq [chan channels]
                (send-message
                 irc bot chan
                 (str "\u0002" owner "/" name "\u0002"
                      ": " (count commits) " new commit(s) on branch " (last (.split ref "/"))
                      ". Compare view at <" (is-gd compare) ">. " (:open_issues repository)
                      " open issues remain."))
                (doseq [commit (take 3 commits)]
                  (notify-chan irc bot chan commit)))))))))
  "These boots are made for walkin' and that's just what they'll do.")

(defplugin
  (:init
   (fn [irc bot]
     (swap! bots assoc (:server @irc) {:irc irc :bot bot})))
  (:routes (POST "/commits" req (handler req))))