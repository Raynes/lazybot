(ns lazybot.plugins.grimoire
  (:require [grimoire.util :as util]
            [lazybot.registry :refer [defplugin send-message]]))

(def- nss #{"clojure.core"
            "clojure.core.protocols"
            "clojure.core.reducers"
            "clojure.data"
            "clojure.edn"
            "clojure.inspector"
            "clojure.instant"
            "clojure.java.browse"
            "clojure.java.io"
            "clojure.java.javadoc"
            "clojure.java.shell"
            "clojure.main"
            "clojure.pprint"
            "clojure.reflect"
            "clojure.repl"
            "clojure.set"
            "clojure.stacktrace"
            "clojure.string"
            "clojure.template"
            "clojure.test"
            "clojure.test.junit"
            "clojure.test.tap"
            "clojure.uuid"
            "clojure.walk"
            "clojure.xml"
            "clojure.zip"})

(defplugin
  (:cmd
   "Print the Grimoire URL for a symbol"
   #{"grim"}
   (fn [{:keys [args] :as com-m}]
     (let [sym      (first args)
           [_ ns s] (re-matches #"(.*?)/(.*)" sym)]
       (send-message
        com-m
        (when (nss ns)
          (format "http://grimoire.arrdem.com/1.6.0/%s/%s"
                  ns (util/munge n))))))))
