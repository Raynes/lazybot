(ns sexpbot.plugins.utils
  (:use [sexpbot utilities [info :only [format-config]] respond gist]
	[clojure.contrib.seq :only [shuffle]]
	[clj-time [core :only [now]] [format :only [unparse formatters]]])
  (:require [irclj.irclj :as ircb]))

(def known-prefixes
     [\& \+ \@ \% \! \~])

(defn drop-modes [users]
  (map (fn [x] (if (some #(= (first x) %) known-prefixes) (apply str (rest x)) x)) users))

(defn pangram? [s]
  (let [letters (into #{} "abcdefghijklmnopqrstuvwxyz")]
    (= (->> s .toLowerCase (filter letters) (into #{})) letters)))

(defmethod respond :time [{:keys [irc nick channel]}]
  (let [time (unparse (formatters :date-time-no-ms) (now))]
    (ircb/send-message irc channel (str nick ": The time is now " time))))

(defmethod respond :join [{:keys [irc channel nick args]}]
  (if-admin nick (ircb/join-chan irc (first args))))

(defmethod respond :part [{:keys [irc nick args channel]}]
  (if-admin nick
	    (let [chan (if (seq args) (first args) channel)]
	      (ircb/send-message irc chan "Bai!")
	      (ircb/part-chan irc chan :reason "Because I don't like you."))))

(defmethod respond :rape [{:keys [args irc channel]}]
  (let [user-to-rape (if (= (first args) "*") 
		       (->> (ircb/get-names irc channel) drop-modes stringify)
		       (first args))]
    (ircb/send-action irc channel (str "raepz " user-to-rape "."))))

(defmethod respond :coin [{:keys [irc nick channel]}]
  (ircb/send-message irc channel (str nick ": " (if (= 0 (rand-int 2)) "Heads." "Tails."))))

(comment (defmethod respond :help [{:keys [irc nick channel]}]
  (ircb/send-message irc channel (str nick ": I can't help you, I'm afraid. You can only help yourself."))))

(defmethod respond :what [{:keys [irc channel]}]
  (ircb/send-message irc channel "It's AWWWW RIGHT!"))

(defmethod respond :pangram [{:keys [irc channel args]}]
  (ircb/send-message irc channel (-> args stringify pangram? str)))

(defmethod respond :fuck [{:keys [irc channel nick]}]
  (ircb/send-message irc channel (str nick ": no u")))

(defmethod respond :setnick [{:keys [irc nick args]}]
  (if-admin nick (ircb/set-nick irc (first args))))

(defmethod respond :exists [{:keys [irc channel args]}]
  (ircb/send-message irc channel (str (.exists (java.io.File. (first args))))))

(defmethod respond :botsnack [{:keys [nick irc channel args]}]
  (ircb/send-message irc channel (str nick ": Thanks! Om nom nom!!")))

(defmethod respond :your [{:keys [irc channel args]}]
  (ircb/send-message irc channel (str (first args) ": It's 'you're', you fucking illiterate bastard.")))

(defmethod respond :kill [{:keys [irc channel]}]
  (ircb/send-message irc channel "IT WITH FIRE. FOR GREAT JUSTICE!"))

(defmethod respond :error [_] (throw (Exception. "Hai!")))

(defmethod respond :gist [{:keys [irc channel nick args]}]
  (ircb/send-message irc channel (str nick ": " 
				      (post-gist (first args) 
					    (->> args rest (interpose " ") (apply str))))))

(defmethod respond :timeout [_]
  (Thread/sleep 15000))

(defmethod respond :dumpcmds [{:keys [irc channel]}]
  (println @commands)
  (ircb/send-message irc channel
		     (->> @commands vals (filter map?) (apply merge) keys 
		     (interpose "\n") (apply str) (post-gist "dumpcmds.clj"))))

(defmethod respond :balance [{:keys [irc channel nick args]}]
  (let [[fst & more] args]
    (ircb/send-message irc channel 
		       (str nick ": " (apply str (concat fst (repeat (count fst) ")")))))))

;;;; Too easy to abuse. ;;;
(defmethod respond :say [{:keys [irc channel nick args]}]
  (if-admin nick
	    (ircb/send-message irc (first args) (->> args rest (interpose " ") (apply str)))))

(defplugin      
  {"time"     :time
   "rape"     :rape
   "coin"     :coin
;   "help"     :help
   "what"     :what
   "pangram?" :pangram
   "join"     :join
   "part"     :part
   "setnick"  :setnick
   "fuck"     :fuck
   "exists?"  :exists
   "botsnack" :botsnack
   "your"     :your
   "kill"     :kill
   "error"    :error
   "gist"     :gist
   "timeout"  :timeout
   "dumpcmds" :dumpcmds
   "balance"  :balance
   "say"      :say})
