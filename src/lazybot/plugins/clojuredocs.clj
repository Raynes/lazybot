(ns lazybot.plugins.clojuredocs
  (:require [cd-client.core :as cd]
            [lazybot.registry :refer [defplugin send-message]]
            [clojure.string :refer [join]]))

(defplugin
  (:cmd
   "Search clojuredocs for something."
   #{"search-cdocs" "cdocs-search" "cd-search"}
   (fn [{:keys [args] :as com-m}]
     (send-message
      com-m
      (if-let [results (seq (take 5 (apply cd/search args)))]
        (join "; " (for [{:keys [url ns name]} (reverse (sort-by :id results))]
                     (format "%s/%s: %s" ns name url)))
        "No results found."))))

  (:cmd
   "Find an example usage of something on clojuredocs."
   #{"examples" "cdocs-examples"}
   (fn [{:keys [args] :as com-m}]
     (send-message
      com-m
      (let [[nspace name :as split-args] (.split (first args) "/" 2)
            args (if name
                   split-args
                   (when-let [resolved (and nspace (-> nspace symbol resolve meta))]
                     (map str ((juxt :ns :name) resolved))))]
        (if-let [results (:url (apply cd/examples-core args))]
          results
          "No results found."))))))