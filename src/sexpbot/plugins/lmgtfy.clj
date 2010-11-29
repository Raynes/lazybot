(ns sexpbot.plugins.lmgtfy
  (:use [sexpbot registry]))

(defn create-url [args]
  (str "http://www.lmgtfy.com/?q=" (apply str (interpose "+" args))))

(defplugin
  (:cmd
   "Constructs a lmgtfy URL. If you attach @ nick at the end, it will direct it towards
   the person named by nick."
   #{"lmgtfy"}
   (fn [{:keys [bot args] :as com-m}]
     (if (not (seq args))
       (send-message com-m "http://www.lmgtfy.com")
       (if (some #(= "@" %) args)
         (let [[url-from user-to] (split-with #(not= "@" %) args)]
           (send-message com-m (str (last user-to) ": " (create-url url-from))))
         (send-message com-m (create-url args)))))))