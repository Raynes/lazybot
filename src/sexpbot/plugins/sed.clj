(ns sexpbot.plugins.sed
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]
	    [sexpbot.core :as core]))

(def prepend (:prepend (read-config)))

; Feel free to write some new ones.
; -- 
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;     OPTIONS       ;;;
;; case insensitive -i ;;
;; don't execute -x    ;;
;; --sed a specific usr;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(defn parse-opts [opt-string]
  {:case-inse (.contains opt-string "i")
   :no-exec (.contains opt-string "x")})
    
(defn sed [string expr options]
  (let [results (rest (re-find #"s/([^/]+)/([^/]*)/" expr))]
    (.replaceAll string (str (if (:case-inse options)"(?i)" "") (first results)) (last results))))

(defmethod respond :sed [{:keys [irc channel args] :as irc-map}]
  (let [conj-args  (apply str (interpose " " args))
	last-in (:last-in @irc)
	opts (parse-opts (if (re-find #"([a-zA-Z]+)[\s]+s/([^/]+)/([^/]*)/" conj-args)
			   (rest (re-matches #"([a-zA-Z]+)[\s]+s/[^/]+/[^/]*/" conj-args))
			   " "))] ;; NAIVE, expects correct input
    (println (str "OPTS:" opts "CONJ:" conj-args))
    (when (= last-in "")
      (ircb/send-message irc channel "No one said anything yet!"))
    (when (re-matches #"([a-zA-Z]*)[\s]+s/[^/]+/[^/]*/" conj-args)
      (ircb/send-message irc channel (sed last-in conj-args opts))
      (when (and (= (first last-in) prepend)
		 (not (:no-exec opts)))
	(core/handle-message (assoc irc-map :message (sed last-in conj-args opts)))))
    (when (not (re-matches #"([a-zA-Z]*)[\s]+s/[^/]+/[^/]*/"  conj-args))
      (ircb/send-message irc channel "Format: sed [options] s/regex/replacement/ Options are: Case Insensitive: i Don't Execute: x"))))

(defplugin {"sed" :sed})
