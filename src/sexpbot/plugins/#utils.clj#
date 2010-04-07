(ns sexpbot.plugins.utils
  (:use (sexpbot utilities commands respond gist privileges)
	[sexpbot.info :only [format-config]]
	(clj-time core format)))

(def known-prefixes
     [\& \+ \@ \% \! \~])

(defn drop-modes [users]
  (map (fn [x] (if (some #(= (first x) %) known-prefixes) (apply str (rest x)) x)) users))

(defn pangram? [s]
  (let [letters (into #{} "abcdefghijklmnopqrstuvwxyz")]
    (= (->> s .toLowerCase (filter letters) (into #{})) letters)))

(defmethod respond :time [{:keys [bot sender channel]}]
  (let [time (unparse (formatters :date-time-no-ms) (now))]
    (.sendMessage bot channel (str sender ": The time is now " time))))

(defmethod respond :join [{:keys [bot sender args]}]
  (if-admin sender (.joinChannel bot (first args))))

(defmethod respond :part [{:keys [bot sender args channel]}]
  (if-admin sender
	    (.sendMessage bot (first args) "Bai!")
	    (.partChannel bot (first args))))

(defmethod respond :rape [{:keys [args bot channel]}]
  (let [user-to-rape (if (= (first args) "*") 
		       (->> (map #(.toString %) (.getUsers bot channel)) drop-modes stringify)
		       (first args))]
    (.sendAction bot channel (str "raepz " user-to-rape "."))))

(defmethod respond :coin [{:keys [bot sender channel]}]
  (.sendMessage bot channel (str sender ": " (if (= 0 (rand-int 2)) "Heads." "Tails."))))

(defmethod respond :help [{:keys [bot sender channel]}]
  (.sendMessage bot channel (str sender ": I can't help you, I'm afraid. You can only help yourself.")))

(defmethod respond :what [{:keys [bot channel]}]
  (.sendMessage bot channel "It's AWWWW RIGHT!"))

(defmethod respond :pangram [{:keys [bot channel args]}]
  (.sendMessage bot channel (-> args stringify pangram? str)))

(defmethod respond :fuck [{:keys [bot channel sender]}]
  (.sendMessage bot channel (str sender ": no u")))

(defmethod respond :setnick [{:keys [bot args]}]
  (.changeNick bot (first args)))

(defmethod respond :exists [{:keys [bot channel args]}]
  (.sendMessage bot channel (str (.exists (java.io.File. (first args))))))

(defmethod respond :botsnack [{:keys [sender bot channel args]}]
  (.sendMessage bot channel (str sender ": Thanks! Om nom nom!!")))

(defmethod respond :your [{:keys [bot channel args]}]
  (.sendMessage bot channel (str (first args) ": It's 'you're', you fucking illiterate bastard.")))

(defmethod respond :kill [{:keys [bot channel]}]
  (.sendMessage bot channel "IT WITH FIRE."))

(defmethod respond :error [_] (throw (Exception. "Hai!")))

(defmethod respond :gist [{:keys [bot channel sender args]}]
  (.sendMessage bot channel (str sender ": " 
				 (post-gist (first args) 
					    (->> args rest (interpose " ") (apply str))))))

(defmethod respond :timeout [_]
  (Thread/sleep 15000))

(defmethod respond :dumpcmds [{:keys [bot channel]}]
  (println @commands)
  (.sendMessage bot channel
		(->> @commands vals (filter map?) (apply merge) keys 
		     (interpose "\n") (apply str) (post-gist "dumpcmds.clj"))))

(defmodule :utils      
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
   "say"      :say
   "error"    :error
   "gist"     :gist
   "timeout"   :timeout
   "dumpcmds"  :dumpcmds})