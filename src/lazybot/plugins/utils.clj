(ns lazybot.plugins.utils
  (:require [irclj.core :as ircb]
            [clojure.string :as s]
            [lazybot.utilities :as utilities]
            [lazybot.info :as info]
            [lazybot.registry :as registry]
            [lazybot.plugins.login :refer [when-privs]]
            [lazybot.paste :refer [trim-with-paste]]
            [clj-time.core :refer [plus minus now interval in-seconds hours]]
            [clj-time.format :refer [unparse formatters]]
            [clojure.java.shell :refer [sh]])
  (:import java.net.InetAddress))

(defn pangram? [s]
  (let [letters (into #{} "abcdefghijklmnopqrstuvwxyz")]
    (= (->> s .toLowerCase (filter letters) (into #{})) letters)))

(registry/defplugin
  (:cmd
   "Gets the current time and date in UTC format."
   #{"time"}
   (fn [{:keys [nick bot args] :as com-m}]
     (let [time (unparse (formatters :date-time-no-ms)
                         (if-let [[[m num]] (seq args)]
                           (let [n (try (Integer/parseInt (str num)) (catch Exception _ 0))]
                             (condp = m
                                 \+ (plus (now) (hours n))
                                 \- (minus (now) (hours n))
                                 (now)))
                           (now)))]
       (registry/send-message com-m (utilities/prefix nick "The time is now " time)))))

  (:cmd
   "Joins a channel. Takes a channel and an optional password. ADMIN ONLY."
   #{"join"}
   (fn [{:keys [com bot nick args] :as com-m}]
     (when-privs com-m :admin
               (ircb/join com (first args) (last args)))))

  (:cmd
   "Parts a channel. Takes a channel and a part message. ADMIN ONLY."
   #{"part"}
   (fn [{:keys [bot com nick args channel] :as com-m}]
     (when-privs com-m :admin
               (let [chan (or (first args) channel)]
                 (registry/send-message com-m "Bai!")
                 (ircb/part com chan :reason "Quit")))))

  (:cmd
   "Flips a coin."
   #{"coin"}
   (fn [{:keys [bot nick] :as com-m}]
     (registry/send-message
      com-m
      (utilities/prefix nick
              (if (zero? (rand-int 2))
                "Heads."
                "Tails.")))))

  (:cmd 
   "Prints an amusing message."
   #{"what"}
   (fn [com-m] (registry/send-message com-m "It's AWWWW RIGHT!")))

  (:cmd
   "Checks if its input string is a pangram."
   #{"pangram?"}
   (fn [{:keys [args] :as com-m}]
     (registry/send-message com-m (->> args s/join pangram? str))))

  (:cmd
   "Just says the sender's name: no u."
   #{"fuck"}
   (fn [{:keys [bot nick] :as com-m}]
     (registry/send-message com-m (utilities/prefix nick "no u"))))

  (:cmd
   "Sets the bot's nick. ADMIN ONLY."
   #{"setnick"}
   (fn [{:keys [com bot nick args] :as com-m}]
     (when-privs com-m :admin (ircb/set-nick com (first args)))))

  (:cmd
   "Love your bot? Give him a snack and thank him for his hard work!"
   #{"botsnack"}
   (fn [{:keys [nick bot] :as com-m}]
     (registry/send-message com-m (utilities/prefix nick "Thanks! Om nom nom!!"))))

  (:cmd
   "Prints an amusing message."
   #{"kill"}
   (fn [com-m] (registry/send-message com-m "KILL IT WITH FIRE!")))

  (:cmd
   "Says what you tell it to in the channel you specify. ADMIN ONLY."
   #{"say"}
   (fn [{:keys [bot nick args] :as com-m}]
     (when-privs com-m :admin
                 (registry/send-message (assoc com-m :channel (first args))
                             (->> args rest (interpose " ") (apply str))))))

  (:cmd
   "Temperature conversion. If given Cn, converts from C to F. If given Fn, converts from F to C."
   #{"tc" "tempconv"}
   (fn [{:keys [nick args bot] :as com-m}]
     (let [num (->> args first rest (apply str) Integer/parseInt)]
       (registry/send-message com-m 
                     (utilities/prefix nick
                             (condp = (ffirst args)
                                 \F (* (- num 32) (/ 5 9.0))
                                 \C (+ 32 (* (/ 9.0 5) num))
                                 "Malformed expression."))))))

  (:cmd
   "Pings an IP address or host name. If it doesn't complete within 10 seconds, it will give up."
   #{"ping"}
   (fn [{:keys [bot nick args] :as com-m}]
     (let [address (InetAddress/getByName (first args))
           stime (now)]
       (registry/send-message
        com-m
        (utilities/prefix nick
             (if (= false (.isReachable address 5000))
               "FAILURE!"
               (str "Ping completed in " (in-seconds (interval stime (now))) " seconds.")))))))

  (:cmd
   "Executes a shell command and displays the STDOUT"
   #{"shell"}
   (fn [{:keys [bot nick args] :as com-m}]
     (when-privs com-m :admin
      (registry/send-message
       com-m
       (let [cmd (s/join " " args)]
         (trim-with-paste
           (s/replace (:out (sh "bash" "-c" cmd)) #"\s+" " ")))))))

  (:cmd
   "SEIZE HIM!"
   #{"guards"}
   (fn [com-m]
     (registry/send-message com-m "SEIZE HIM!"))))
