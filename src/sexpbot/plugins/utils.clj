(ns sexpbot.plugins.utils
  (:use [sexpbot.respond]))

(def known-prefixes
     [\& \+ \@ \% \! \~])

(defn drop-modes [users]
  (map (fn [x] (if (some #(= (first x) %) known-prefixes) 
		 (apply str (rest x))
		 x)) users))

(defmethod respond :time [_ this sender chan _ _]
  (let [time (.toString (java.util.Date.))]
    (.sendMessage this chan (str sender ": The time is now " time))))

(defmethod respond :quit [_ this _ chan _ _]
  (.sendMessage this chan "Okay, I'm fucking leaving. Fuck.")
  (System/exit 0))

(defmethod respond :rape [{[m & _] :args} this _ chan _ _]
  (let [user-to-rape (if (= m "*") 
		       (apply str (interpose 
				   " " 
				   (drop-modes (map #(.toString %) (.getUsers this chan)))))
		       m)]
    (.sendAction this chan (str "raepz " user-to-rape "."))))