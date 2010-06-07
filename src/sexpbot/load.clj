(ns sexpbot.load
  (:use [sexpbot respond [info :only [info-file]]]
	[clj-config.core :only [read-config]]
	[clojure.set :only [intersection]]))

(def all-plugins (:plugins (read-config info-file)))

(defn loadmod [irc modu]
  (when ((:modules @irc) (-> modu keyword))
    ((((:modules @irc) (-> modu keyword)) :load)) true))

(def initial-hooks
  {:core {:on-message [(fn [irc-map] (try-handle irc-map))]
	  :on-quit []
	  :on-join []}})

(defn require-plugins []
  (doseq [plug ((read-config info-file) :plugins)]
    (let [prefix (str "sexpbot.plugins." plug)]
      (require (symbol prefix) :reload))))

(defn load-plugins [irc]
  (let [info (read-config info-file)
	plugins-to-load (intersection (:plugins info) (:plugins (info (:server @irc))))]
    (doseq [plug plugins-to-load]
      (let [prefix (str "sexpbot.plugins." plug)]
	((resolve (symbol (str prefix "/load-this-plugin"))) irc)))))

(defn load-modules
  "Loads all of the modules in the IRC's :module map in another thread."
  [irc]
  (doseq [plug (:plugins ((read-config info-file) (:server @irc)))]
    (.start (Thread. (fn [] (loadmod irc plug))))))

(defn reload-all!
  "A clever function to reload everything when running sexpbot from SLIME.
  Do not try to reload anything individually. It doesn't work because of the
  way refs are used. This makes sure everything is reset to the way it was
  when the bot was first loaded."
  [irc]
  (dosync
   (alter irc assoc :hooks initial-hooks)
   (alter irc assoc :commands {})
   (doseq [cfn (map :cleanup (vals (:modules @irc)))] (cfn))
   (alter irc assoc :modules {}))
  (use 'sexpbot.respond :reload)
  (require-plugins)
  (load-plugins irc)
  (load-modules irc))