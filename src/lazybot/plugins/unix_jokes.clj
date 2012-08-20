(ns lazybot.plugins.unix-jokes
  (:use (lazybot registry info))
  (:require [clojure.string :as s]))

(defmulti response (fn [[command & args] com-m]
                     command))
(defmethod response :default [_ _] nil)

(let [min 5, max 15
      ls (fn [^String dir]
           (let [root (java.io.File. dir)]
             (sort
              (take (+ min (rand-int (- max min)))
                    (shuffle (seq (.list root)))))))]
  (defmethod response "ls" [_ com-m]
    (s/join " " (ls "/"))))

(defmethod response "whoami" [_ com-m]
  (:nick com-m))

(defmethod response "mutt" [_ _]
  "Woof!")

(defmethod response "pwd" [_ com-m]
  (:channel com-m))

(defmethod response "echo" [[command & args] _]
  (when args
    (s/join " " args)))

(defplugin
  (:hook
   :on-message
   (fn [{:keys [bot com message channel] :as com-m}]
     (when-let [reply (response (s/split message #"\s+") com-m)]
       (send-message com-m reply)))))
