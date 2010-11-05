(ns sexpbot.plugins.utils
  (:use [sexpbot utilities info respond]
	[clj-github.gists :only [new-gist]]
	clj-config.core
	[clj-time [core :only [plus minus now interval in-secs hours]] [format :only [unparse formatters]]]
        [clojure.java.shell :only [sh]])
  (:require [irclj.irclj :as ircb])
  (:import java.net.InetAddress))

(def known-prefixes
     [\& \+ \@ \% \! \~])

(defn drop-modes [users]
  (map (fn [x] (if (some #(= (first x) %) known-prefixes) (apply str (rest x)) x)) users))

(defn pangram? [s]
  (let [letters (into #{} "abcdefghijklmnopqrstuvwxyz")]
    (= (->> s .toLowerCase (filter letters) (into #{})) letters)))

(defplugin
  (:cmd
   "Gets the current time and date in UTC format."
   #{"time"} 
   (fn [{:keys [irc bot nick channel args]}]
     (let [time (unparse (formatters :date-time-no-ms) 
                         (if-let [[[m num]] (seq args)]
			 
                           (let [n (try (Integer/parseInt (str num)) (catch Exception _ 0))] 
                             (condp = m
                                 \+ (plus (now) (hours n))
                                 \- (minus (now) (hours n))
                                 (now)))
                           (now)))]
       (send-message irc bot channel (str nick ": The time is now " time)))))

  (:cmd 
   "Joins a channel. Takes a channel and an optional password. ADMIN ONLY."
   #{"join"}
   (fn [{:keys [irc bot channel nick args] :as irc-map}]
     (if-admin nick irc-map bot
               (ircb/join-chan irc (first args) (last args)))))

  (:cmd
   "Parts a channel. Takes a channel and a part message. ADMIN ONLY." 
   #{"part"} 
   (fn [{:keys [irc bot nick args channel] :as irc-map}]
     (if-admin nick irc-map bot
               (let [chan (if (seq args) (first args) channel)]
                 (send-message irc bot chan "Bai!")
                 (ircb/part-chan irc chan :reason "Because I don't like you.")))))

  (:cmd 
   "Rapes a person you specify."
   #{"rape"} 
   (fn [{:keys [args bot irc channel]}]
     (ircb/send-action irc channel (str "raepz " (first args) "."))))

  (:cmd 
   "Flips a coin."
   #{"coin"} 
   (fn [{:keys [irc bot nick channel]}]
     (send-message irc bot channel (str nick ": " (if (= 0 (rand-int 2)) "Heads." "Tails.")))))

  (:cmd 
   "Prints an amusing message."
   #{"what"} 
   (fn [{:keys [irc bot channel]}]
     (send-message irc bot channel "It's AWWWW RIGHT!")))
   
  (:cmd 
   "Checks if it's input string is a pangram."
   #{"pangram?"} 
   (fn [{:keys [irc bot channel args]}]
     (send-message irc bot channel (-> args stringify pangram? str))))
   
  (:cmd 
   "Just says the sender's name: no u."
   #{"fuck"} 
   (fn [{:keys [irc bot channel nick]}]
     (send-message irc bot channel (str nick ": no u"))))

  (:cmd
   "Sets the bot's nick. ADMIN ONLY."
   #{"setnick"} 
   (fn [{:keys [irc bot channel nick args] :as irc-map}]
     (if-admin nick irc-map bot (ircb/set-nick irc (first args)))))

  (:cmd
   "Amusing command to check to see if a directory exists on the system that runs the bot."
   #{"exists?"} 
   (fn [{:keys [irc bot channel args]}]
     (send-message irc bot channel (str (.exists (java.io.File. (first args)))))))

  (:cmd
   "Love your bot? Give him a snack and thank him for his hard work!"
   #{"botsnack"} 
   (fn [{:keys [nick irc bot channel args]}]
     (send-message irc bot channel (str nick ": Thanks! Om nom nom!!"))))
   
  (:cmd 
   "Prints an amusing and inappropriate message directed at a person you specify. For when people
    use 'your' when they should have used 'you're'"
   #{"your"} 
   (fn [{:keys [irc bot channel args]}]
     (send-message irc bot channel (str (first args) ": It's 'you're', you fucking illiterate bastard."))))

  (:cmd 
   "Prints an amusing message."
   #{"kill"}
   (fn [{:keys [irc bot channel]}]
     (send-message irc bot channel "KILL IT WITH FIRE!")))

  (:cmd 
   "Gists it's arguments."
   #{"gist"} 
   (fn [{:keys [irc bot channel nick args]}]
     (send-message irc bot channel (str nick ": http://gist.github.com/" 
                                        (:repo (new-gist {}
                                                         (first args) 
                                                         (->> args rest (interpose " ") (apply str)))))))

  ;(:dumpcmds
  ; "Dumps a list of commands to a gist."
  ; ["dumpcmds" "commands"]
  ; [{:keys [irc channel]}]
  ; (send-message
  ;  irc channel
  ;  (str "http://gist.github.com/"
  ;       (:repo (new-gist "dumpcmds"
  ;                        ((constantly "omg") ( apply str
  ;                                                    (for [[x {doc :doc}] (->> (:commands @irc) vals (apply merge))]
  ;                                                      (str x ": " doc "\n")))))))))

   (:cmd 
    "Balances parens for you."
    #{"balance"}
    (fn [{:keys [irc bot channel nick args]}]
      (let [[fst & more] args]
        (send-message irc bot channel 
                      (str nick ": " (apply str (concat fst (repeat (count fst) ")")))))))))

  (:cmd 
   "Says what you tell it to in the channel you specify. ADMIN ONLY."
   #{"say"} 
   (fn [{:keys [irc bot channel nick args] :as irc-map}]
     (if-admin nick irc-map bot
               (send-message irc bot (first args) (->> args rest (interpose " ") (apply str))))))

  (:cmd
   "Temperature conversion. If given Cn, converts from C to F. If given Fn, converts from F to C."
   #{"tc" "tempconv"}
   (fn [{:keys [irc bot channel nick args]}]
     (let [num (->> args first rest (apply str) Integer/parseInt)]
       (send-message irc bot channel 
                     (str nick ": "
                          (condp = (ffirst args)
                              \F (* (- num 32) (/ 5 9.0))
                              \C (+ 32 (* (/ 9.0 5) num))
                              "Malformed expression."))))))
  
  (:cmd
   "Pings an IP address or host name. If it doesn't complete within 10 seconds, it will give up."
   #{"ping"}
   (fn [{:keys [irc bot channel nick args]}]
     (let [address (InetAddress/getByName (first args))
           stime (now)]
       (send-message 
        irc bot channel 
        (str nick ": "
             (if (= false (.isReachable address 5000))
               "FAILURE!"
               (str "Ping completed in " (in-secs (interval stime (now))) " seconds.")))))))

  (:cmd
   "Huggles your best fwiendz."
   #{"huggle"}
   (fn [{:keys [irc bot channel args]}]
     (ircb/send-action irc channel (str "Hugglez " (first args) ". I lubs yous."))))

  (:cmd
   "I'd do you."
   #{"would"}
   (fn [{:keys [irc bot channel]}] (send-message irc bot channel "I'd do him. Hard.")))

  (:cmd
   "Executes a shell command and displays the STDOUT"
   #{"shell"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin
      nick irc-map bot
      (send-message
       irc bot channel
       (->> args rest (interpose " ") (apply str) (sh (first args)) :out
            (take 200) (apply str) (#(.replace % "\n" " "))))))))
