(ns sexpbot.registry
  (:use [amalloy.utils :only [verify validator !]]
        [amalloy.utils.transform :only [transform-if]]
        [sexpbot.utilities :only [on-thread]]
        [clojail.core :only [thunk-timeout]])
  (:require [irclj.core :as ircb]
            [somnium.congomongo :as mongo])
  (:import java.util.concurrent.TimeoutException))

(defmacro def- [name value]
  `(def ~(vary-meta name assoc :private true) ~value))

(defn nil-comp [com bot channel s action? & fns]
  (reduce #(when %1
             (%2 com bot channel %1 action?))
          s fns))

(defn equal-nil [x y] (or (= x y) (nil? y)))

(defn remove-protos [proto s]
  (filter
   (fn [[k v]] (equal-nil proto (:protocol v)))
   s))

(defn pull-hooks [bot hook-key]
  (map :fn
       (hook-key
        (apply merge-with concat
               (map :hooks
                    (vals
                     (remove-protos (:protocol @bot) (:modules @bot))))))))

(defn extract-protocol [m & rest]
  (-> ((transform-if map? :bot) m)
      deref :protocol))

(defn call-message-hooks [com bot channel s action?]
  (apply nil-comp com bot channel s action? (pull-hooks bot :on-send-message)))

(defmulti send-message #'extract-protocol)

(defmethod send-message :irc
  [{:keys [com bot channel]} s & {:keys [action? notice?]}]
  (if-let [result (call-message-hooks com bot channel s action?)]
    ((cond
      action? ircb/send-action
      notice? ircb/send-notice
      :else ircb/send-message)
     com channel result)))

(defmulti prefix
  "Multiple protocol safe name prefixing"
  #'extract-protocol)

(defmethod prefix :irc
  [bot nick & s]
  (apply str nick ": " s))

(defn find-command [modules command]
  (some (validator #((:triggers %) command))
        (apply concat (map :commands (vals modules)))))

(defn find-docs [bot command]
  (:docs (find-command (:modules @bot) command)))

(defn respond [{:keys [command bot]}]
  (let [cmd (find-command (remove-protos (:protocol @bot)
                                         (:modules @bot))
                          command)]
    (or (and (equal-nil (:protocol @bot)
                        (:protocol cmd))
             (:fn cmd))
        (constantly nil))))

(defn full-prepend [config s]
  ((:prepends config) s))

(defn m-starts-with [m s]
  (some #(.startsWith m %) s))

(defn split-args [config s no-pre?]
  (let [[prepend command & args] (.split s " ")
        is-long-pre (full-prepend config prepend)
        prefix (or (full-prepend config prepend)
                   (->> prepend first str (full-prepend config)))]
    {:command (cond
               is-long-pre command
               prefix (apply str (rest prepend))
               no-pre? prepend)
     :args (if is-long-pre
             args
             (when command
               (conj args command)))}))

(defn is-command?
  "Tests whether or not a message begins with a prepend."
  [message bot]
  (m-starts-with message (-> @bot :config :prepends)))

(defn try-handle [{:keys [nick channel bot message] :as com-m}]
  (on-thread
   (let [conf (:config @bot)
         max-ops (:max-operations conf)
         no-pre? (or (= nick channel) (= :twitter (:protocol @bot)))]
     (when (or no-pre? (is-command? message bot))
       (if (dosync
            (let [pending (:pending-ops @bot)
                  permitted (< pending max-ops)]
              (when permitted
                (alter bot assoc :pending-ops (inc pending)))))
         (try
           (let [n-bmap (into com-m (split-args conf message no-pre?))]
             (thunk-timeout #((respond n-bmap) n-bmap)
                            30 :sec))
           (catch TimeoutException _ (send-message com-m "Execution timed out."))
           (catch Exception e (.printStackTrace e))
           (finally
            (dosync
             (alter bot assoc :pending-ops (dec (:pending-ops @bot))))))
         (send-message com-m "Too much is happening at once. Wait until other operations cease."))))))

(defn merge-with-conj [& args]
  (apply merge-with #(if (vector? %) (conj % %2) (conj [] % %2)) args))

(defn parse-fns [body protocol]
  (apply merge-with-conj
         (for [[one & [two three four five :as args]] body]
           {one
            (case
             one
             :cmd {:docs two
                   :triggers three
                   :fn (or five four)
                   :protocol (or protocol (verify keyword? four))}
             :hook {two {:fn (or four three)
                         :protocol (or protocol (verify keyword? three))}}
             :indexes (vec args)
             two)})))

(defn if-seq-error [fn-type possible-seq]
  (if (and (not (fn? possible-seq)) (seq possible-seq))
    (throw (Exception. (str "Only one " fn-type " function allowed.")))
    possible-seq))

;; Wrap isolated objects with a vector
(def make-vector (transform-if (! vector?) vector))

(defmacro defplugin [& [protocol & body]]
  (let [proto (verify keyword? protocol)
        checked-body (if-not (keyword? protocol) (cons protocol body) body)
        {:keys [cmd hook cleanup init indexes routes]} (parse-fns checked-body proto)
        scmd (if (map? cmd) [cmd] cmd)]
    `(let [pns# *ns*
           m-name# (keyword (last (.split (str pns#) "\\.")))]
       (defn ~'load-this-plugin [com# bot#]
         (when ~init ((if-seq-error "init" ~init) com# bot#))
         (doseq [idx# ~indexes]
           (apply mongo/add-index! m-name# idx#))
         (dosync
          (alter bot# assoc-in [:modules m-name#]
                 {:protocol ~proto
                  :commands ~scmd
                  :hooks (into {}
                               (for [[k# v#] (apply merge-with-conj
                                                    (make-vector ~hook))]
                                 [k# (make-vector v#)]))
                  :cleanup (if-seq-error "cleanup" ~cleanup)
                  :routes ~routes}))))))
