(ns lazybot.plugins.shorturl
  (:require [lazybot.registry :as registry]
            [hobbit.core :as hobbit]
            [hobbit.bitly :as bitly]
            [hobbit.isgd :as isgd]))

(registry/defplugin
  (:cmd
   "Shorten a URL. Pass the name of the service you want to use followed by the url."
   #{"shorten"}
   (fn [{:keys [bot args] :as com-m}]
     (registry/send-message
      com-m
      (if-let [short (hobbit/shortener (first args) (-> @bot :config :shorturl :auth))]
        (hobbit/shorten short (second args))
        "No URL shortener by that name was found."))))

  (:cmd
   "Expand a shortened URL. Pass a URL and I will discover the service you're using automatically."
   #{"expand"}
   (fn [{:keys [bot args] :as com-m}]
     (registry/send-message
      com-m
      (if-let [short (hobbit/shortener (first args) (:hobbit-auth (:config @bot)))]
        (hobbit/expand short (first args))
        "No URL shortener by that name was found.")))))
		 
