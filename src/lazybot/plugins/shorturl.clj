(ns lazybot.plugins.shorturl
  (:use lazybot.registry
        [hobbit core bitly isgd]))

(defplugin
  (:cmd
   "Shorten a URL. Pass the name of the service you want to use followed by the url."
   #{"shorten"}
   (fn [{:keys [bot args] :as com-m}]
     (send-message
      com-m
      (if-let [short (shortener (first args) (-> @bot :config :shorturl :auth))]
        (shorten short (second args))
        "No URL shortener by that name was found."))))

  (:cmd
   "Expand a shortened URL. Pass a URL and I will discover the service you're using automatically."
   #{"expand"}
   (fn [{:keys [bot args] :as com-m}]
     (send-message
      com-m
      (if-let [short (shortener (first args) (:hobbit-auth (:config @bot)))]
        (expand short (first args))
        "No URL shortener by that name was found.")))))
		 
