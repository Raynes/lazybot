(ns sexpbot.plugins.sed
  (:use [sexpbot respond info])
  (:require [irclj.irclj :as ircb]))

(def prepend (:prepend (read-config)))

; Feel free to write some new ones.
; -- 
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;     OPTIONS       ;;;
;; case insensitive -i ;;
;; don't execute -x    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-opts [opt-string]
  {:case-inse (.contains opt-string "i")
   :no-exec (.contains opt-string "x")})
    
(defn sed [string expr options]
  (let [results (rest (re-find #"s/([^/]+)/([^/]*)/" expr))]
    (.replaceAll string (str (if (:case-inse options) "(?i)" "") (first results)) (last results))))

(defmethod respond :sed [{:keys [irc channel args] :as irc-map}]
  (let [[farg & margs] args
	conj-args  (apply str (interpose " " margs))
	last-in ((:last-in @irc) (if (= \# (first farg)) farg))
	opts (parse-opts (if (re-find #"([a-zA-Z]+)[\s]+s/([^/]+)/([^/]*)/" conj-args)
			   (rest (re-matches #"([a-zA-Z]+)[\s]+s/[^/]+/[^/]*/" conj-args))
			   " "))]
    (cond (empty? last-in) (ircb/send-message irc channel "No one said anything there yet!")
	  (re-matches #"([a-zA-Z]*)[\s]+s/[^/]+/[^/]*/" conj-args) 
	  (do (ircb/send-message irc channel (sed last-in conj-args opts))
	      (when (and (= (first last-in) prepend)
			 (not (:no-exec opts)))
		(handle-message (assoc irc-map :message (sed last-in conj-args opts)))))
	  :else (ircb/send-message irc channel "Format: sed channel [options] s/regex/replacement/ Options are: Case Insensitive: i Don't Execute: x"))))

(defplugin {"sed" :sed})
