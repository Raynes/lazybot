(ns sexpbot.plugins.utils
  (:use (sexpbot utilities commands respond)
	(clj-time core format)))

(def known-prefixes
     [\& \+ \@ \% \! \~])

(defn drop-modes [users]
  (map (fn [x] (if (some #(= (first x) %) known-prefixes) 
		 (apply str (rest x))
		 x)) users))

(defn pangram? [s]
  (let [letters (into #{} "abcdefghijklmnopqrstuvwxyz")
	text (->> s .toLowerCase (filter letters) (into #{}))]
    (= text letters)))

(def util-cmds
     {"time"     :time
      "rape"     :rape
      "coin"     :coin
      "help"     :help
      "what"     :what
      "pangram?" :pangram
      "join"     :join
      "part"     :part})

(defmethod respond :time [{:keys [bot sender channel]}]
  (let [time (unparse (formatters :date-time-no-ms) (now))]
    (.sendMessage bot channel (str sender ": The time is now " time))))

(defmethod respond :join [{:keys [bot args]}]
  (.joinChannel bot (first args)))

(defmethod respond :part [{:keys [bot args channel]}]
  (.sendMessage bot channel "Bai!")
  (.partChannel bot (first args)))

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


(defmodule util-cmds :utils)