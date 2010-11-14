(ns sexpbot.plugins.debug
  (:use sexpbot.plugin)
  (:require [clojure.string :as string]))

(defn get-mode [old-mode user-arg]
  (case user-arg
        :on :on
        :off nil
        :-v :verbose
        :? old-mode
        (when (nil? old-mode) ; toggle
          :on)))

(defn debug-mode-str [mode]
  (or (name mode)
      "off"))

(def debug-keypath [:configs :debug-mode])

(defplugin
  (:cmd
   "Toggle debug mode, causing full stacktraces to be printed to the console/log when an exception goes uncaught, as well as permitting evaluation of arbitrary clojure code to inspect the running bot. Use the -v flag to print more debug information to the log."
   #{"debug"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (if-admin
      nick irc-map bot
      (send-message irc bot channel
                    (dosync
                     (let [arg (keyword (first args))]
                       (alter bot update-in debug-keypath get-mode arg)
                       (str "Debug mode: "
                            (debug-mode-str
                             (get-in @bot debug-keypath)))))))))
  (:cmd
   "Evaluate code in an un-sandboxed context. Prints the result to stdout, does not send it to any irc channel."
   #{"deval"}
   (fn [{:keys [irc bot nick channel args] :as irc-map}]
     (when (get-in @bot debug-keypath) (if-admin
       nick irc-map bot
       (let [code (string/join " " args)]
         (println (str code " evaluates to:\n" (eval (read-string code))))))))))