(ns sexpbot.core
  (:use [sexpbot respond info utilities]
	[clojure.stacktrace :only [root-cause]]
	[clojure.contrib.string :only [split]])
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

(defn split-args [s] (let [[command & args] (split #" " s)]
		       {:command command
			:first (first command)
			:args args}))


(defn handle-message [chan send login host mess server irc]
  (let [bot-map {:bot irc
		 :sender send
		 :channel chan
		 :login login
		 :host host
		 :server server
		 :privs (get-priv send)}]
    (if (= (first mess) prepend)
      (-> bot-map (merge (->> mess rest (apply str) split-args)) respond))))

(defn try-handle [chan send login host mess server irc]
  (try
   (thunk-timeout 
    #(try
      (handle-message chan send login host mess server irc)
      (catch Exception e 
	(println (str e))))
    30)
   (catch TimeoutException _
     (ircb/send-message irc chan "Execution Timed Out!"))))

(defn get-links [s]
  (->> s (re-seq #"(http://|www\.)[^ ]+") (apply concat) (take-nth 2)))

(defn on-message [chan send login host [begin & more :as mess] server irc]
  (when (not (((info :user-blacklist) server) send))
    (let [links (get-links mess)
	  title-links? (and (not= prepend begin) 
			    (catch-links? server)
			    (seq links)
			    (find-ns 'sexpbot.plugins.title))
	  message (if title-links? 
		    (str prepend "title2 " (apply str (interpose " " links)))
		    mess)]
      (try-handle chan send login host message server irc))))

(defn make-bot-run [name pass server]
  (let [fnmap {:on-message (fn [{:keys [channel nick ident hmask message irc]}] 
				    (on-message channel nick ident hmask message server irc))
		      :on-quit (fn [{:keys [nick ident hmask message irc]}]
				 (when (find-ns 'sexpbot.plugins.login) 
				   (try-handle nick nick ident hmask (str prepend "quit") server irc)))
		      :on-join (fn [{:keys [channel nick ident hmask irc]}]
				    (when (find-ns 'sexpbot.plugins.mail)
				      (try-handle channel nick ident hmask 
						  (str prepend "mailalert") server irc)))}]
    (ircb/create-bot {:name name :password pass :server server :fnmap fnmap})))

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