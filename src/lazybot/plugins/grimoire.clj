(ns lazybot.plugins.grimoire
  (:require [lazybot.registry :as registry]))

(def core-re #"clojure\.((core)|(data)|(edn)|(inspector)|(java)|(main)|(pprint)|(repl)|(set)|(stacktrace)|(template)|(test)|(uuid)|(walk)|(xml)|(zip)).*")

(registry/defplugin
  (:cmd
   "Print the Grimoire URL for a symbol"
   #{"grim"}
   (fn [{:keys [args] :as com-m}]
     (println "args" args)
     (let [sym (first args)]
       (when (re-matches core-re sym)
         (let [s  (read-string sym)
               ns (namespace s)
               n  (name s)]
           (when (and name namespace)
             (registry/send-message
              com-m
              (format "http://grimoire.arrdem.com/1.6.0/%s/%s" ns n)))))))))
