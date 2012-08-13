(ns lazybot.plugins.clojuredocs
  (:use lazybot.registry))

(def clojuredocs-url "http://clojuredocs.org/")

(defn full-name [fn-name]
  "Converts function name to vector [ns name].
  Returns nil if cannot resolve function or it is not a function (e.g. class)."
  (let [{:keys [ns name]} (-> fn-name symbol resolve meta)]
    (when (and ns name)
      (map str [(.getName ns) name]))))

(defn escape-fn-name [name]
  ; Replace special characters like ?, /, space, .
  (->> (clojure.string/lower-case name)
       (replace {\? "_q"
                 \/ "_"
                 \space "_"
                 \. "_dot"})
       (apply str)))

(defn build-search-url [name]
  ; First replace / with spaces, because clojuredocs doesn't like / in search queries.
  (->> (clojure.string/replace name "/" " ")
       (java.net.URLEncoder/encode)
       (str clojuredocs-url "search?q=")))

(defn build-fn-url [[ns name]]
  (str clojuredocs-url "clojure_core/" ns "/" (escape-fn-name name)))

(defn build-url [name]
  (if-let [full-name (full-name name)]
    ; Resolved function - show url to this function.
    (build-fn-url full-name)
    ; Unknown function - show search url.
    (build-search-url name)))

(defplugin
  (:cmd
   "Shows URL to clojuredocs pages. Examples: \"cljdoc even?\", \"cljdocs clojure.string/escape\",
   \"clojuredoc clojure.string join\"."
   #{"clojuredoc" "clojuredocs" "cljdoc" "cljdocs"}
   (fn [{:keys [args] :as m-map}]
     ; First join all args with / so "clojure.core +" becomes "clojure.core/+"
     (->> (interpose "/" args)
          (apply str)
          build-url
          (send-message m-map)))))