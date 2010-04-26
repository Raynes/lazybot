(ns sexpbot.core
  (:use [sexpbot respond info utilities]
	[clojure.stacktrace :only [root-cause]])
  (:require [org.danlarkin.json :as json]
	    [irclj.irclj :as ircb])
  (:import [java.io File FileReader]
	   java.util.concurrent.TimeoutException))

(def info (read-config))
(def prepend (:prepend info))
(def servers (:servers info))
(def plugins (:plugins info))
(def catch-links? (:catch-links? info))

; Require all plugin files listed in info.clj
(doseq [plug plugins] (->> plug (str "sexpbot.plugins.") symbol require))

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

(defn on-message [{:keys [nick channel message irc] :as irc-map}]
  (when (and (not= nick (:name @irc))
	     (not= (take 4 message) (cons (:prepend info) "sed")))
    (dosync (alter irc assoc :last-in {channel message})))
  (when (not (((info :user-blacklist) (:server @irc)) nick))
    (let [links (get-links message)
	  title-links? (and (not= prepend (first message)) 
			    (catch-links? (:server @irc))
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
	irc (ircb/connect (make-bot-run name pass server) :channels channels :identify-after-secs 3)]
    irc))

(doseq [plug plugins] (.start (Thread. (fn [] (loadmod plug)))))
(doseq [server servers] (make-bot server))
