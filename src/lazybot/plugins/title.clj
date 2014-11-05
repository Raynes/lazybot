;; The result of a team effort between programble and Rayne.
(ns lazybot.plugins.title
  (:require [lazybot.info :as info]
            [lazybot.registry :as registry]
            [lazybot.utilities :as utilities]
            [clojure.java.io :refer [reader]]
            [clojure.string :refer [triml]]
            [clojure.tools.logging :refer [debug]]
            [clojail.core :refer [thunk-timeout]])
  (:import java.util.concurrent.TimeoutException
           org.apache.commons.lang.StringEscapeUtils))

(def titlere #"(?i)<title>([^<]+)</title>")

(defn collapse-whitespace [s]
  (->> s (.split #"\s+") (interpose " ") (apply str)))

(defn add-url-prefix [url]
  (if-not (.startsWith url "http")
    (str "http://" url)
    url))

(defn slurp-or-default [url]
  (try
   (with-open [readerurl (reader url)]
     (loop [acc [] lines (line-seq readerurl)]
       (cond
        (not (seq lines)) nil
        (some #(re-find #"</title>|</TITLE>" %) acc) (->> acc (apply str)
                                                          (#(.replace % "\n" " "))
                                                          (re-find titlere))
        :else (recur (conj acc (first lines)) (rest lines)))))
   (catch java.lang.Exception e nil)))

(defn url-blacklist-words [network bot] (:url-blacklist ((:config @bot) network)))

(defn url-check [network bot url]
  (some #(.contains url %) (url-blacklist-words network bot)))

(defn strip-tilde [s] (apply str (remove #{\~} s)))

(defn title [{:keys [network nick bot user channel] :as com-m}
             links & {verbose? :verbose?}]
  (if (or (and verbose? (seq links))
          (not (contains? (get-in @bot [:config network :title :blacklist])
                          channel)))
    (doseq [link (take 1 links)]
      (try
       (thunk-timeout #(let [url (add-url-prefix link)
                             page (slurp-or-default url)
                             match (second page)]
                         (if (and (seq page) (seq match) (not (url-check network bot url)))
                           (registry/send-message com-m
                                              (str "\""
                                                   (triml
                                                    (StringEscapeUtils/unescapeHtml
                                                     (collapse-whitespace match)))
                                                   "\""))
                           (when verbose? (registry/send-message com-m "Page has no title."))))
                      20 :sec)
       (catch TimeoutException _
         (when verbose?
           (registry/send-message com-m "It's taking too long to find the title. I'm giving up.")))))
    (when verbose? (registry/send-message com-m "Which page?"))))

(registry/defplugin
  (:hook
   :on-message
   (fn [{:keys [network bot nick channel message] :as com-m}]
     (let [info (:config @bot)
           get-links (fn [s]
                       (->> s
                            (re-seq #"(https?://|www\.)[^\]\[(){}\"'$^\s]+")
                            (map first)))]
       (let [prepend (:prepends info)
             links (get-links message)
             title-links? (and (not (registry/is-command? message prepend))
                               (get-in info [network :title :automatic?])
                               (seq links))]
         (when title-links?
           (title com-m links))))))

  (:cmd
   "Gets the title of a web page. Takes a link. This is verbose, and prints error messages."
   #{"title"} (fn [com-m] (title com-m (:args com-m) :verbose? true))))
