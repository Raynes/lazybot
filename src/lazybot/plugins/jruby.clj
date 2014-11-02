(ns lazybot.plugins.jruby
  (:require [lazybot.registry :as registry]
            [clojail.core :refer [thunk-timeout]]
            [clojure.stacktrace :as stacktrace])
  (:import (javax.script ScriptEngineManager ScriptException)
           java.io.StringWriter
           org.jruby.exceptions.RaiseException))

(def jruby (.getEngineByName (ScriptEngineManager.) "jruby"))

(defn eval-jruby [code]
  (thunk-timeout
   (fn []
     (let [writer (StringWriter.)]
       (.setWriter (.getContext jruby) writer)
       (str writer " "
            (try (pr-str (.eval jruby code))
                 (catch ScriptException e
                   (.getMessage (stacktrace/root-cause e)))))))
   5000))

(registry/defplugin
  (:cmd
   "Evaluate JRuby code."
   #{"jr"}
   (fn [{:keys [raw-args] :as com-m}]
     (registry/send-message com-m (eval-jruby raw-args)))))
