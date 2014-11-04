(ns lazybot.plugins.metacritic
  (:require [lazybot.registry :refer [send-message defplugin]]
            [me.raynes.laser :as l]
            [clojure.string :as s]))

(def paths
  {"xbox"  "/game/xbox-360/"
   "pc"    "/game/pc/"
   "ps3"   "/game/playstation-3/"
   "movie" "/movie/"})

(defn normalize [s]
  (s/join "-" (map s/lower-case s)))

(def url "http://www.metacritic.com")

(defn metacritic [args]
  (let [[type & args] args
        path (str url (paths type) (normalize args))]
    (if-let [html (try (let [html (l/parse (slurp path))]
                         (when-not (seq (l/select html (l/class= "error_code")))
                           html))
                       (catch Exception e (println "metacritic got error" e "looking for" path)))]
      (let [[critic user] (map (comp l/text first)
                               [(l/select html 
                                          (l/descendant-of
                                           (l/class= "main_details")
                                           (l/class= "score_value")))
                                (l/select html
                                          (l/descendant-of
                                           (l/class= "userscore_wrap")
                                           (l/class= "score_value")))])]
        (str "Critics: " critic "; User: " user "  -  " path))
      "I'm a little drunk. Can't find my keys.")))

(defplugin
  (:cmd
   "Look up scores for a game or movie on metacritic. Pass the type of thing it is.
    This can be movie, xbox, ps3, or pc."
   #{"metacritic"}
   (fn [{:keys [args] :as com-m}]
     (send-message com-m (metacritic args)))))
