(ns lazybot.plugins.roulette
  (:use [lazybot.registry]))

(defn make-pistol [size n]
  "Returns a random seq of false and true of len size with n true"
  (shuffle (concat (take n (repeat true)) (take (- size n) (repeat false)))))

(def pistol (atom []))

(defn load-pistol [& args]
  "Assigns (make-pistol ...) to pistol atom"
  (reset! pistol (apply make-pistol args)))

(defplugin

  (:cmd
    "Load pistol"
    #{"load-pistol" "rload"}
    (fn [{:keys [args] :as com-m}]
      (if-let [[size n] args]
        (load-pistol (Integer/parseInt size) (Integer/parseInt n))
        (load-pistol 6 1))
      (send-message com-m "The pistol has been loaded.")))

  (:cmd
    "Loads pistol for lethality"
    #{"load-lethal" "load-die"}
    (fn [com-m]
      (load-pistol 8 6)
      (send-message com-m "The pistol has been loaded, prepare to die.")))

  (:cmd
    "Pulls the trigger"
    #{"pull"}
    (fn [{:keys [nick] :as com-m}]
      (when (seq @pistol)
        (if (peek @pistol)
          (send-message com-m "drags away the body" :action? true)
          (send-message com-m (str nick " gets to live for now")))
        (swap! pistol pop)))))
