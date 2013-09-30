(ns lazybot.plugins.stocks
  (:use [lazybot registry info]
        [lazybot.utilities :only [prefix]]
        [clojure.string :only (split)])
  (:require [clj-http.client :as client]))

(def base "http://download.finance.yahoo.com/d/quotes.csv?f=price&s=")

(defn price-by-symbol [symbol]
  (let [response (client/get (str base symbol))
        components (split (:body response) #",")
        price (first components)]
    price))

(defplugin
  (:hook
    :on-message
    (fn [{:keys [com bot nick message channel] :as com-m}]
      (when-let [[match value] (re-find #"(?:.*\s)?(\$[a-zA-Z]+).*" message)]
        (when-let [price (price-by-symbol (subs value 1))]
          (send-message com-m (str value ": " price)))))))
