(ns lazybot.plugins.grimoire
  (:require [grimoire.util :as util]
            [grimoire.things :as t]
            [grimoire.either :as e]
            [grimoire.api :as api]
            [grimoire.api.web :as web]
            [grimoire.api.web.read]
            [lazybot.registry :refer [defplugin send-message]]))

(def -config
  (web/->Config "http://conj.io"))

(def def-index
  (atom {}))

(defn set-def-index! []
  (let [group (t/->Group "org.clojure")]
    (->> (for [artifact     (e/result (api/list-artifacts -config group))
               :let [newest (first (e/result (api/list-versions -config artifact)))]
               platform     (e/result (api/list-platforms -config newest))
               ns           (e/result (api/list-namespaces -config platform))
               def          (e/result (api/list-defs -config ns))]
           [(str (t/thing->name platform) "::" (t/thing->name ns) "/" (t/thing->name def)) def])
         (into {})
         (reset! def-index))))

(set-def-index!)

(defplugin
  (:cmd
   "Print the Grimoire URL for a symbol"
   #{"grim"}
   (fn [{:keys [args] :as com-m}]
     (let [sym                   (first args)
           [sym key platform ns s] (re-matches #"((.+)::(.+))/(.+)" sym)]
       (->> (if (and platform ns s)
              (if-let [def (get @def-index sym)]
                (str "⇒ " (web/make-html-url -config def))
                (str "Failed to find " sym))
              (str "Identify a def with <platform>::<namespace>/<name>"))
            (send-message com-m)))))

  ;; FIXME: this should probably be ratelimited
  (:cmd
   "Reload the Grimoire ns index"
   #{"reload-grim"}
   (fn [com-m]
     (->> (try (do (set-def-index!)
                   (format "Reload succeeded!, %d defs indexed."
                           (count @def-index)))
               (catch Exception e
                 (str "Reload failed!" (.getMessage e))))
          (send-message com-m)))))
