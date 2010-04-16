(ns sexpbot.plugins.lmgtfy
  (:use [sexpbot respond])
  (:require [irclj.irclj :as ircb]))

(defn create-url [args]
  (str "http://www.lmgtfy.com/?q=" (apply str (interpose "+" args))))

(defmethod respond :lmgtfy [{:keys [bot channel args]}]
  (if (not (seq args))
    (ircb/send-message bot channel "http://www.lmgtfy.com")
    (if (some #(= "@" %) args)
      (let [[url-from user-to] (split-with #(not= "@" %) args)]
	(ircb/send-message bot channel (str (last user-to) ": " (create-url url-from))))
      (ircb/send-message bot channel (create-url args)))))

(defplugin {"lmgtfy" :lmgtfy})