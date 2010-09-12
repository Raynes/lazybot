(ns sexpbot.load
  (:use [sexpbot respond [info :only [info-file]]]
	[clj-config.core :only [read-config]]
	[clojure.set :only [intersection]]))

(def all-plugins (:plugins (read-config info-file)))

(def initial-hooks
     {:on-message [(fn [irc-map] (try-handle irc-map))]
      :on-quit []
      :on-join []})

(defn reload-config [bot]
  (alter bot assoc :config (read-config info-file)))

(defn require-plugin [plugin]
  (require (symbol (str "sexpbot.plugins." plugin)) :reload))

(defn load-plugin [irc refzors plugin]
  ((resolve (symbol (str (str "sexpbot.plugins." plugin) "/load-this-plugin"))) irc refzors))

(defn require-plugins []
  (doseq [plug ((read-config info-file) :plugins)]
    (require-plugin plug)))

(defn safe-load-plugin [refzors plugin]
  (try
    (load-plugin refzors plugin)
    true
    (catch Exception e false)))

(defn load-plugins [irc refzors]
  (let [info (:config @refzors)
	plugins-to-load (intersection (:plugins info) (:plugins (info (:server @irc))))]
    (doseq [plug plugins-to-load]
      (load-plugin irc refzors plug))))

(defn reload-configs
  "Reloads the bot's configs. Must be ran in a transaction."
  [& bots]
  (doseq [[_ bot] bots]
    (reload-config bot)))

(defn reload-all
  "A clever function to reload everything when running sexpbot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the nature
  of plugins. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  [& bots]
  (doseq [{bot :bot} bots]
    (doseq [{:keys [cleanup]} (vals (:modules @bot))]
      (when cleanup (cleanup)))
    (dosync
     (alter bot dissoc :modules)
     (alter bot assoc-in [:modules :internal :hooks] initial-hooks)
     (reload-config bot)))
  (require-plugins)
  (doseq [{:keys [bot irc]} bots]
    (load-plugins irc bot)))