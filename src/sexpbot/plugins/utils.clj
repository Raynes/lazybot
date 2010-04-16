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

(defmethod respond :time [{:keys [bot sender channel]}]
  (let [time (unparse (formatters :date-time-no-ms) (now))]
    (ircb/send-message bot channel (str sender ": The time is now " time))))

(defmethod respond :join [{:keys [bot channel sender args]}]
  (if-admin sender (ircb/join-chan bot (first args))))

(defmethod respond :part [{:keys [bot sender args channel]}]
  (if-admin sender
	    (let [chan (if (seq args) (first args) channel)]
	      (ircb/send-message bot chan "Bai!")
	      (ircb/part-chan bot chan))))

(defmethod respond :rape [{:keys [args bot channel]}]
  (let [user-to-rape (if (= (first args) "*") 
		       (->> (ircb/get-names bot channel) drop-modes stringify)
		       (first args))]
    (ircb/send-action bot channel (str "raepz " user-to-rape "."))))

(defmethod respond :coin [{:keys [bot sender channel]}]
  (ircb/send-message bot channel (str sender ": " (if (= 0 (rand-int 2)) "Heads." "Tails."))))

(defmethod respond :help [{:keys [bot sender channel]}]
  (ircb/send-message bot channel (str sender ": I can't help you, I'm afraid. You can only help yourself.")))

(defmethod respond :what [{:keys [bot channel]}]
  (ircb/send-message bot channel "It's AWWWW RIGHT!"))

(defmethod respond :pangram [{:keys [bot channel args]}]
  (ircb/send-message bot channel (-> args stringify pangram? str)))

(defmethod respond :fuck [{:keys [bot channel sender]}]
  (ircb/send-message bot channel (str sender ": no u")))

(defmethod respond :setnick [{:keys [bot sender args]}]
  (if-admin sender
	    (.changeNick bot (first args))))

(defmethod respond :exists [{:keys [bot channel args]}]
  (ircb/send-message bot channel (str (.exists (java.io.File. (first args))))))

(defmethod respond :botsnack [{:keys [sender bot channel args]}]
  (ircb/send-message bot channel (str sender ": Thanks! Om nom nom!!")))

(defmethod respond :your [{:keys [bot channel args]}]
  (ircb/send-message bot channel (str (first args) ": It's 'you're', you fucking illiterate bastard.")))

(defmethod respond :kill [{:keys [bot channel]}]
  (ircb/send-message bot channel "IT WITH FIRE. FOR GREAT JUSTICE!"))

(defmethod respond :error [_] (throw (Exception. "Hai!")))

(defmethod respond :gist [{:keys [bot channel sender args]}]
  (ircb/send-message bot channel (str sender ": " 
				 (post-gist (first args) 
					    (->> args rest (interpose " ") (apply str))))))

(defmethod respond :timeout [_]
  (Thread/sleep 15000))

(defmethod respond :dumpcmds [{:keys [bot channel]}]
  (println @commands)
  (ircb/send-message bot channel
		(->> @commands vals (filter map?) (apply merge) keys 
		     (interpose "\n") (apply str) (post-gist "dumpcmds.clj"))))

(defmethod respond :balance [{:keys [bot channel sender args]}]
  (let [[fst & more] args]
    (ircb/send-message bot channel 
		  (str sender ": " (apply str (concat fst (repeat (count fst) ")")))))))

;;;; Too easy to abuse. ;;;
(defmethod respond :say [{:keys [bot channel sender args]}]
  (if-admin sender
    (ircb/send-message bot (first args) (->> args rest (interpose " ") (apply str)))))

(defmethod respond :loginandsuch [{:keys [bot channel login host]}]
  (ircb/send-message bot channel (str login "|" host)))

(defplugin      
  {"time"     :time
   "rape"     :rape
   "coin"     :coin
   "help"     :help
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
   "say"      :say
   "testing"  :loginandsuch})
