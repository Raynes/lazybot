(ns sexpbot.plugins.time
  (:use [sexpbot.respond]))

(defmethod respond :time [_ this sender chan _ _]
  (let [time (.toString (java.util.Date.))]
    (.sendMessage this chan (str sender ": The time is now " time))))