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
    (if-let [html (try (l/parse (slurp path))
                       (catch Exception _))]
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
   "Look up scores for a game, movie, or album on metacritic. Pass the type of thing it is.
    This can be movie, xbox, ps3, pc, or album."
   #{"metacritic"}
   (fn [{:keys [args] :as com-m}]
     (send-message com-m (metacritic args)))))