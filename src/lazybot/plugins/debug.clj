(ns lazybot.plugins.debug
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [lazybot.registry :as registry]
            [lazybot.plugins.login :refer [when-privs]]))

(defn get-mode [old-mode user-arg]
  (case user-arg
    :on :on
    :off nil
    :-v :verbose
    :? old-mode
    (when-not old-mode              ; toggle
      :on)))

(defn debug-mode-str [mode]
  (if mode
    (name mode)
    "off"))

(def debug-keypath [:configs :debug-mode])

(registry/defplugin
  (:cmd
   "Toggle debug mode, causing full stacktraces to be printed to the console/log when an exception goes uncaught, as well as permitting evaluation of arbitrary clojure code to inspect the running bot. Use the -v flag to print more debug information to the log."
   #{"debug"}
   (fn [{:keys [bot nick channel args] :as com-m}]
     (when-privs com-m :admin
      (registry/send-message com-m
                    (dosync
                     (let [arg (keyword (first args))]
                       (alter bot update-in debug-keypath get-mode arg)
                       (str "Debug mode: "
                            (debug-mode-str
                             (get-in @bot debug-keypath)))))))))
  (:cmd
   "Evaluate code in an un-sandboxed context. Prints the result to stdout, does not send it to any irc channel."
   #{"deval"}
   (fn [{:keys [bot nick channel args] :as com-m}]
     (when (get-in @bot debug-keypath)
       (when-privs com-m :admin
        (let [code (string/join " " args)]
          (println (str code " evaluates to:\n"
                        (with-out-str
                          (pprint/pprint (eval (read-string code))))))))))))
