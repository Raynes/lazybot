(ns lazybot.plugins.jruby
  (:use lazybot.registry
        [clojail.core :only [thunk-timeout]])
  (:import javax.script.ScriptEngineManager
           java.io.StringWriter))

(def jruby (.getEngineByName (ScriptEngineManager.) "jruby"))

(defn eval-jruby [code]
  (thunk-timeout
   (fn []
     (let [writer (StringWriter.)]
       (.setWriter (.getContext jruby) writer)
       (str writer " " (pr-str (.eval jruby code)))))
   5000))

(defplugin
  (:cmd
   "Evaluate JRuby code."
   #{"jr"}
   (fn [{:keys [raw-args] :as com-m}]
     (send-message com-m (eval-jruby raw-args)))))