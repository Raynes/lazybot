(ns sexpbot.core
  (:use [sexpbot respond info utilities]
	[clojure.stacktrace :only [root-cause]]
	[clojure.contrib.string :only [split]])
  (:require [org.danlarkin.json :as json]
	    [irclj.irclj :as ircb])
  (:import [java.io File FileReader]
	   java.util.concurrent.TimeoutException
	   java.lang.String ))

(def info (read-config))
(def prepend (:prepend info))
(def servers (:servers info))
(def plugins (:plugins info))
(def catch-links? (:catch-links? info))

(def last-in "") ; to hold the last line of input

; Require all plugin files listed in info.clj
(doseq [plug plugins] (->> plug (str "sexpbot.plugins.") symbol require))

(defn split-args [s] (let [[command & args] (split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

(defn handle-message [{:keys [nick message] :as irc-map}]
  (let [bot-map (assoc irc-map :privs (get-priv nick))]
    (if (= (first message) prepend)
      (-> bot-map (into (->> message rest (apply str) split-args)) respond))))

(defn try-handle [{:keys [nick channel irc] :as irc-map}]
  (try
   (thunk-timeout 
    #(try
      (handle-message irc-map)
      (catch Exception e 
	(.printStackTrace e)))
    30)
   (catch TimeoutException _
     (ircb/send-message irc channel "Execution Timed Out!"))))

(defn get-links [s]
  (->> s (re-seq #"(http://|www\.)[^ ]+") (apply concat) (take-nth 2)))

(defn on-message [{:keys [nick message irc] :as irc-map}]
  (when (and (not= nick (:name @irc)) ; rem to change
	     (not= (take 4 message) (cons (:prepend info) "sed")))  (def last-in message))
  (when (not (((info :user-blacklist) (:server @irc)) nick))
    (let [links (get-links message)
	  title-links? (and (not= prepend (first message)) 
			   ; (catch-links? (:server @irc))
			    (seq links)
			    (find-ns 'sexpbot.plugins.title))
	  mess (if title-links? 
		 (str prepend "title2 " (apply str (interpose " " links)))
		 message)]
      (try-handle (assoc irc-map :message mess)))))

(defn make-bot-run [name pass server]
  (let [fnmap {:on-message (fn [irc-map] 
			     (when (find-ns 'sexpbot.plugins.seen)
			       (try-handle (assoc irc-map
					     :message (str prepend "putseen*")
					     :extra-args ["talking"])))
			     (on-message irc-map))
	       :on-quit (fn [irc-map]
			  (when (find-ns 'sexpbot.plugins.seen)
			    (try-handle (assoc irc-map 
					  :message (str prepend "putseen*") 
					  :extra-args ["quitting"])))
			  (when (find-ns 'sexpbot.plugins.login) 
			    (try-handle (assoc irc-map :message (str prepend "quit")))))
	       :on-join (fn [irc-map]
			  (when (find-ns 'sexpbot.plugins.seen)
			    (try-handle (assoc irc-map 
					  :message (str prepend "putseen*")
					  :extra-args ["joining"])))
			  (when (find-ns 'sexpbot.plugins.mail)
			    (try-handle (assoc irc-map :message (str prepend "mailalert")))))}]
    (ircb/create-irc {:name name :password pass :server server :fnmap fnmap})))

(defn make-bot [server] 
  (let [bot-config (read-config)
	name ((bot-config :bot-name) server)
	pass ((bot-config :bot-password) server)
	channels ((bot-config :channels) server)
	irc (ircb/connect (make-bot-run name pass server) :channels channels)]
    (when (seq pass)
      (Thread/sleep 3000)
      (ircb/send-message irc "NickServ" (str "identify " pass))
      (println "Sleeping while identification takes effect.")
      (Thread/sleep 3000))
    (doseq [plug plugins] (.start (Thread. (fn [] (loadmod plug)))))))

(doseq [server servers] (.start (Thread. (fn [] (make-bot server)))))
