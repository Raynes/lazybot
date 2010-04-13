(ns sexpbot.core
  (:use [sexpbot respond info]
	[clojure.stacktrace :only [root-cause]]
	[clojure.contrib.str-utils :only [re-split]])
  (:require [org.danlarkin.json :as json])
  (:import [org.jibble.pircbot PircBot]
	   [java.io File FileReader]
	   [java.util.concurrent FutureTask TimeUnit TimeoutException]))

(def info (read-config))
(def prepend (:prepend info))
(def servers (:servers info))
(def plugins (:plugins info))
(def catch-links? (:catch-links? info))

; Require all plugin files listed in info.clj
(doseq [plug plugins] (->> plug (str "sexpbot.plugins.") symbol require))

(defn wall-hack-method [class-name name- params obj & args]
  (-> class-name (.getDeclaredMethod (name name-) (into-array Class params))
    (doto (.setAccessible true))
    (.invoke obj (into-array Object args))))

(defn split-args [s] (let [[command & args] (re-split #" " s)]
		       {:command command
			:first (first command)
			:args args}))

;;;;;; From clojurebot's sandbox.clj, adapted for my code. ;;;;;;
(defn thunk-timeout [thunk seconds]
      (let [task (FutureTask. thunk)
            thr (Thread. task)]
        (try
          (.start thr)
          (.get task seconds TimeUnit/SECONDS)
          (catch TimeoutException e
                 (.cancel task true)
                 (.stop thr (Exception. "Thread stopped!")) 
		 (throw (TimeoutException. "Execution Timed Out"))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-message [chan send login host mess server this]
  (let [bot-map {:bot this
		 :sender send
		 :channel chan
		 :login login
		 :host host
		 :server server
		 :privs (get-priv send)}]
    (if (= (first mess) prepend)
      (-> bot-map (merge (->> mess rest (apply str) split-args)) respond))))

(defn try-handle [chan send login host mess server this]
  (try
   (thunk-timeout 
    #(try
      (handle-message chan send login host mess server this)
      (catch Exception e 
	(println (str e))))
    30)
   (catch TimeoutException _
     (.sendMessage this chan "Execution Timed Out!"))))

(defn get-links [s]
  (->> s (re-seq #"(http://|www\.)[^ ]+") (apply concat) (take-nth 2)))

(defn on-message [chan send login host [begin & more :as mess] server this]
  (when (not (((info :user-blacklist) server) send))
    (let [links (get-links mess)
	  title-links? (and (not= prepend begin) 
			    (catch-links? server)
			    (seq links)
			    (find-ns 'sexpbot.plugins.title))
	  message (if title-links? 
		    (str prepend "title2 " (apply str (interpose " " links)))
		    mess)]
      (try-handle chan send login host message server this))))

(defn make-bot-obj [server]
  (proxy [PircBot] []
    (onMessage 
     [chan send login host mess] (on-message chan send login host mess server this))
    (onPrivateMessage
     [send login host message] (on-message send send login host message server this))
    (onQuit
     [send login host message]
     (when (find-ns 'sexpbot.plugins.login) 
       (try-handle send send login host (str prepend "quit") server this)))
    (onJoin
     [chan send login host]
     (when (find-ns 'sexpbot.plugins.mail)
       (try-handle chan send login host (str prepend "mailalert") server this)))))

(defn make-bot [server] 
  (let [bot (make-bot-obj server)
	bot-config (read-config)
	name ((bot-config :bot-name) server)
	pass ((bot-config :bot-password) server)
	channels ((bot-config :channels) server)]
    (wall-hack-method PircBot :setName [String] bot name)
    (doto bot
      (.setVerbose true)
      (.connect server))
    (when (seq pass)
      (Thread/sleep 3000)
      (.sendMessage bot "NickServ" (str "identify " pass))
      (println "Sleeping while identification takes effect.")
      (Thread/sleep 3000))
    (doseq [chan channels] (.start (Thread. (fn [] (.joinChannel bot chan)))))
    (doseq [plug plugins] (.start (Thread. (fn [] (loadmod plug)))))))

(doseq [server servers] (.start (Thread. (fn [] (make-bot server)))))