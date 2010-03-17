(ns sexpbot.plugins.lmgtfy
  (:use (sexpbot respond commands)))

(def lmgtfy-cmds {"lmgtfy" :lmgtfy})

(defn create-url [args]
  (str "http://www.lmgtfy.com/?q=" (apply str (interpose "+" args))))

(defmethod respond :lmgtfy [{:keys [bot channel args]}]
  (if (not (seq args))
    (.sendMessage bot channel "http://www.lmgtfy.com")
    (if (some #(= "@" %) args)
      (let [[url-from user-to] (split-with #(not= "@" %) args)]
	(.sendMessage bot channel (str (last user-to) ": " (create-url url-from))))
      (.sendMessage bot channel (create-url args)))))

(defmodule lmgtfy-cmds :lmgtfy)