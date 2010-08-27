(ns sexpbot.load
  (:use [sexpbot respond [info :only [info-file]]]
	[clj-config.core :only [read-config]]
	[clojure.set :only [intersection]]))

(def all-plugins (:plugins (read-config info-file)))

(defn loadmod [bot modu]
  (when ((:modules @bot) (-> modu keyword))
    ((((:modules @bot) (-> modu keyword)) :load)) true))

(def initial-hooks
     {:core {:on-message [(fn [irc-map] (try-handle irc-map))]
             :on-quit []
             :on-join []}})

(defn require-plugins []
  (doseq [plug ((read-config info-file) :plugins)]
    (let [prefix (str "sexpbot.plugins." plug)]
      (require (symbol prefix) :reload))))

(defn load-plugins [server refzors]
  (let [info (read-config info-file)
	plugins-to-load (intersection (:plugins info) (:plugins (info server)))]
    (doseq [plug plugins-to-load]
      (let [prefix (str "sexpbot.plugins." plug)]
	((resolve (symbol (str prefix "/load-this-plugin"))) refzors)))))

(defn load-modules
  "Loads all of the modules in the IRC's :module map in another thread."
  [server refzors]
  (doseq [plug (:plugins ((read-config info-file) server))]
    (.start (Thread. (fn [] (loadmod refzors plug))))))

(defn reload-all!
  "A clever function to reload everything when running sexpbot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the
  way refs are used. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  [& bots]
  (doseq [[_ bot] bots]
    (dosync
     (alter bot assoc :hooks initial-hooks :commands {})
     (doseq [cfn (map :cleanup (vals (:modules @bot)))] (cfn))
     (alter bot assoc :modules {})))
  (use 'sexpbot.respond :reload)
  (require-plugins)
  (doseq [[server bot] bots]
    (load-plugins server bot)
    (load-modules server bot)))