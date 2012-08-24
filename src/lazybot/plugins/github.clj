(ns lazybot.plugins.github
  (:require [lazybot.registry :refer [send-message defplugin]]
            [lazybot.utilities :refer [shorten-url]]
            [compojure.core :refer [POST]]
            [clojure.data.json :refer [read-json Read-JSON-From]]
            [clojure.string :as s]
            [tentacles.issues :refer [specific-issue]]
            [useful.map :refer [update]]
            [clojure.pprint :refer [pprint]])
  (:import java.net.InetAddress))

(def bots (atom {}))

(extend nil Read-JSON-From {:read-json-from (constantly nil)})

(defn grab-config [] (-> @bots vals first :bot deref :config :github))

(defn format-vec [v]
  (let [[show hide] (split-at 10 v)]
    (s/join "" ["[" (s/join ", " show)
              (when (seq hide) "...") "]"])))

(defn notify-chan [com-m commit owner name branch no-header action]
  "Use data gleaned from (handler) to build alert message to rooms
specified in config.clj."
  (send-message
   com-m
   (let [{:keys [added removed modified message url]} commit]
     (s/join "" [(when no-header
                   (str "\u0002" owner "/" name "\u0002 " action branch " <" (shorten-url url) "> "))
                 "\u0002" (-> commit :author :name) "\u0002: "
                 (format-vec (concat modified (map #(str "+" %) added) (map #(str "-" %) removed)))
                 " \u0002--\u0002 " (s/replace message #"\n" " ")]))))

(defn handler [req]
  "Process the JSON payload sent by Github's post-commit hooks."
  (let [remote (:remote-addr req)]
    (when (or (= "127.0.0.1" remote)
              (.endsWith (.getCanonicalHostName (InetAddress/getByName remote)) "github.com"))
      ;; Though `req` is a proper Clojure map, its :form-params key has a
      ;; JSON string as its value, which we parse out into a map.
      (let [{:keys [before repository commits after compare ref deleted] :as payload}
            (read-json ((:form-params req) "payload"))
            config (:commits (grab-config))]
        (when-let [conf (and payload (config (:url repository)))]
          (doseq [[server channels] conf]
            (let [{:keys [com bot] :as com-map} (@bots server)
                  owner (-> repository :owner :name)
                  name (:name repository)
                  commit-type (case (second (.split ref "/"))
                                "heads" {:upper "Branch" :lower "branch"}
                                "tags" {:upper "Tag" :lower "tag"}
                                "remotes" {:upper "Remote" :lower "remote"})
                  n-commits (count commits)
                  branchdel? (true? deleted)
                  ;; If it's a new branch, the "before" field will be a str
                  ;; consisting of a number of zeroes.
                  branchnew? (if (re-find #"^0+$" before) true false)
                  no-header (or (:no-header conf) (= n-commits 1))
                  branch (last (.split ref "/"))]
              (doseq [chan channels]
                (let [com-m (assoc com-map :channel chan)]
                  (when-not no-header
                    (send-message
                     com-m
                     (str "\u0002" owner "/" name "\u0002"
                          ": " (cond
                                branchdel? (str (:upper commit-type) " deleted: " branch ".")
                                branchnew? (str (:upper commit-type) " created: " branch ".")
                                :else (str " -- " (count commits) " new commit(s) on " (:lower commit-type) " " branch ". Compare view at <" (shorten-url compare) ">."))
                          ;; Only report issues if there are some issues
                          ;; to report.
                          (when (> (:open_issues repository) 0)
                            (str " " (:open_issues repository) " open issues remain.")))))
                  ;; Grab first three commits from the message and pass to
                  ;; (notify-chan) to squawk.
                  (doseq [commit (take 3 commits)]
                    (let [action (cond
                                  branchnew? "New branch: "
                                  branchdel? "Branch deleted: "
                                  :else nil)]
                      (notify-chan com-m commit owner name branch no-header action)))))))))))
    (str
     "These boots are made for walkin' and that's just what they'll do. "
     "One of these days these boots are gonna walk all over you."))

(def issue-regex #"\w+\/\w+#\d+")

(defn parse-issue
  "Parse an issue message into its user, repo, and issue number parts."
  [s]
  (-> (zipmap [:user :repo :issue] 
              (s/split s #"\/|#"))
      (update :issue #(Long. %))))

(defn issue-message
  "Create a message containing a link to the issue and the summary of
   the issue."
  [issue]
  (let [issue (specific-issue (:user issue) (:repo issue) (:issue issue))]
    (when-not (= 404 (:status issue))
      (apply format "%s -- %s is %s" ((juxt :title :html_url :state) issue)))))

(defplugin
  (:init
   (fn [com bot]
     (swap! bots assoc (:server @com) {:com com :bot bot})))
  (:routes (POST "/commits" req (handler req)))

  (:hook
   :on-message
   (fn [{:keys [message nick bot com] :as com-m}]
     (when-not ((get-in @bot [:config (:server @com) :user-blacklist]) nick)
       (when-let [match (re-find issue-regex message)]
         (when-let [message (issue-message (parse-issue match))]
           (send-message com-m message)))))))