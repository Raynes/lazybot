(ns sexpbot.core
  (:use [sexpbot respond info]
	[clojure.stacktrace :only [root-cause]]
	[clojure.contrib.str-utils :only [re-split]])
  (:require [org.danlarkin.json :as json])
  (:import [org.jibble.pircbot PircBot]
	   [java.io File FileReader]
	   [java.util.concurrent FutureTask TimeUnit TimeoutException]))

(let [info (read-config)]
  (def prepend (:prepend info))
  (def servers (:servers info))
  (def plugins (:plugins info)))

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

(defn handle-message [chan send login host mess this]
  (let [bot-map {:bot this
		 :sender send
		 :channel chan
		 :login login
		 :host host
		 :privs (get-priv send)}]
    (if (= (first mess) prepend)
      (-> bot-map (merge (->> mess rest (apply str) split-args)) respond))))

(defn try-handle [chan send login host mess this]
  (try
   (thunk-timeout 
    #(try
      (handle-message chan send login host mess this)
      (catch Exception e 
	(println (str e))))
    20)
   (catch TimeoutException _
     (.sendMessage this chan "Execution Timed Out!"))))

(defn make-bot-obj []
  (proxy [PircBot] []
    (onMessage 
     [chan send login host mess] (try-handle chan send login host mess this))
    (onPrivateMessage
     [send login host message] (try-handle send send login host message this))
    (onQuit
     [send login host message]
     (when (find-ns 'sexpbot.plugins.login) 
       (try-handle send send login host (str prepend "quit") this)))
    (onJoin
     [chan send login host]
     (when (find-ns 'sexpbot.plugins.mail)
       (try-handle chan send login host (str prepend "mailalert") this)))))

(defn make-bot [server] 
  (let [bot (make-bot-obj)
	bot-config (read-config)
	name ((bot-config :bot-name) server)
	pass ((bot-config :bot-password) server)
	channels ((bot-config :channels) server)]
    (wall-hack-method PircBot :setName [String] bot name)
    (doto bot
      (.setVerbose true)
      (.connect server))
    (when (seq pass)
      (Thread/sleep 2000)
      (.sendMessage bot "NickServ" (str "identify " pass))
      (println "Sleeping while identification takes effect.")
      (Thread/sleep 2000))
    (doseq [chan channels] (.joinChannel bot chan))
    (doseq [plug plugins] (loadmod plug))))

(doseq [server servers] (make-bot server))