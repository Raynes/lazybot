(ns sexpbot.plugins.utils
  (:use [sexpbot utilities info respond]
	clj-gist.core
	clj-config.core
	[clj-time [core :only [plus minus now interval in-secs hours]] [format :only [unparse formatters]]])
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
  (:time 
   "Gets the current time and date in UTC format."
   ["time"] 
   [{:keys [irc nick channel args]}]
   (let [time (unparse (formatters :date-time-no-ms) 
		       (if-let [[[m num]] (seq args)]
			 
			 (let [n (try (Integer/parseInt (str num)) (catch Exception _ 0))] 
			   (condp = m
			     \+ (plus (now) (hours n))
			     \- (minus (now) (hours n))
			     (now)))
			 (now)))]
     (ircb/send-message irc channel (str nick ": The time is now " time))))

  (:join 
   "Joins a channel. ADMIN ONLY."
   ["join"] 
   [{:keys [irc channel nick args] :as irc-map}]
   (if-admin nick irc-map
	     (ircb/join-chan irc (first args))))
   
  (:part 
   "Parts a channel. Takes a channel and a part message. ADMIN ONLY." 
   ["part"] 
   [{:keys [irc nick args channel] :as irc-map}]
   (if-admin nick irc-map
	     (let [chan (if (seq args) (first args) channel)]
	       (ircb/send-message irc chan "Bai!")
	       (ircb/part-chan irc chan :reason "Because I don't like you."))))

  (:rape 
   "Rapes a person you specify."
   ["rape"] 
   [{:keys [args irc channel]}]
   (let [user-to-rape (if (= (first args) "*") 
			(->> (ircb/get-names irc channel) drop-modes stringify)
			(first args))]
     (ircb/send-action irc channel (str "raepz " user-to-rape "."))))

  (:coin 
   "Flips a coin."
   ["coin"] 
   [{:keys [irc nick channel]}]
   (ircb/send-message irc channel (str nick ": " (if (= 0 (rand-int 2)) "Heads." "Tails."))))

  (:what 
   "Prints an amusing message."
   ["what"] 
   [{:keys [irc channel]}]
   (ircb/send-message irc channel "It's AWWWW RIGHT!"))
   
  (:pangram 
   "Checks if it's input string is a pangram."
   ["pangram?"] 
   [{:keys [irc channel args]}]
   (ircb/send-message irc channel (-> args stringify pangram? str)))
   
  (:fuck 
   "Just says the sender's name: no u."
   ["fuck"] 
   [{:keys [irc channel nick]}]
   (ircb/send-message irc channel (str nick ": no u")))

  (:setnick 
   "Sets the bot's nick. ADMIN ONLY."
   ["setnick"] 
   [{:keys [irc channel nick args] :as irc-map}]
   (if-admin nick irc-map (ircb/set-nick irc (first args))))

  (:exists 
   "Amusing command to check to see if a directory exists on the system that runs the bot."
   ["exists?"] 
   [{:keys [irc channel args]}]
   (ircb/send-message irc channel (str (.exists (java.io.File. (first args))))))

  (:botsnack 
   "Love your bot? Give him a snack and thank him for his hard work!"
   ["botsnack"] 
   [{:keys [nick irc channel args]}]
   (ircb/send-message irc channel (str nick ": Thanks! Om nom nom!!")))
   
  (:your 
   "Prints an amusing and inappropriate message directed at a person you specify. For when people
    use 'your' when they should have used 'you're'"
   ["your"] 
   [{:keys [irc channel args]}]
   (ircb/send-message irc channel (str (first args) ": It's 'you're', you fucking illiterate bastard.")))

  (:kill 
   "Prints an amusing message."
   ["kill"]
   [{:keys [irc channel]}]
   (ircb/send-message irc channel "IT WITH FIRE. FOR GREAT JUSTICE!"))
   
  (:error "" ["error"] [_] (throw (Exception. "Hai!")))

  (:gist 
   "Gists it's arguments."
   ["gist"] 
   [{:keys [irc channel nick args]}]
   (ircb/send-message irc channel (str nick ": " 
				       (post-gist (first args) 
						  (->> args rest (interpose " ") (apply str))))))
   

  (:timeout "" ["timeout"] [_] (Thread/sleep 15000))

  (:dumpcmds 
   "Dumps a list of commands to a gist."
   ["dumpcmds"]
   [{:keys [irc channel]}]
   (ircb/send-message irc channel
		      (->> (:commands @irc) vals (filter map?) (apply merge) keys 
			   (interpose "\n") (apply str) (post-gist "dumpcmds.clj"))))

  (:balance 
   "Balances parens for you."
   ["balance"]
   [{:keys [irc channel nick args]}]
   (let [[fst & more] args]
     (ircb/send-message irc channel 
			(str nick ": " (apply str (concat fst (repeat (count fst) ")")))))))

  (:say 
   "Says what you tell it to in the channel you specify. ADMIN ONLY."
   ["say"] 
   [{:keys [irc channel nick args] :as irc-map}]
   (if-admin nick irc-map
	     (ircb/send-message irc (first args) (->> args rest (interpose " ") (apply str)))))

  (:tempconv
   "Temperature conversion. If given Cn, converts from C to F. If given Fn, converts from F to C."
   ["tc" "tempconv"]
   [{:keys [irc channel nick args]}]
   (let [num (->> args first rest (apply str) Integer/parseInt)]
     (ircb/send-message irc channel 
			(str nick ": "
			     (condp = (ffirst args)
			       \F (* (- num 32) (/ 5 9.0))
			       \C (+ 32 (* (/ 9.0 5) num))
			       "Malformed expression.")))))
  
  (:ping
   "Pings an IP address or host name. If it doesn't complete within 10 seconds, it will give up."
   ["ping"]
   [{:keys [irc channel nick args]}]
   (let [address (InetAddress/getByName (first args))
	 stime (now)]
     (ircb/send-message 
      irc channel 
      (str nick ": "
	   (if (= false (.isReachable address 5000))
	     "FAILURE!"
	     (str "Ping completed in " (in-secs (interval stime (now))) " seconds."))))))

  (:huggle
   "Huggles your best fwiendz."
   ["huggle"]
   [{:keys [irc channel args]}]
   (ircb/send-action irc channel (str "Hugglez " (first args) ". I lubs yous."))))
