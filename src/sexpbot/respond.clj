(ns sexpbot.respond)

(def commands
     {"time" :time
      "quit" :quit
      "rape" :rape})

(defn cmd-respond [{:keys [command]} & _] (commands command))

(defmulti respond cmd-respond)