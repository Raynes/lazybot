(ns sexpbot.respond
  (:use [sexpbot.commands]))

(defn find-command [cmds command]
  (let [result (apply merge (remove keyword? (vals cmds)))]
    (if (cmds command) (cmds command) (result command))))

(defn cmd-respond [{:keys [command]}] (find-command @commands command))

(defmulti respond cmd-respond)

(defmethod respond :load [{:keys [args]}]
  (((@modules (keyword (first args))) :load)))

(defmethod respond :unload [{:keys [args]}]
  (((@modules (keyword (first args))) :unload)))

(defmethod respond :default [_]
  (println "Not found."))