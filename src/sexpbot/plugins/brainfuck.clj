(ns lazybot.plugins.brainfuck
  (:use lazybot.registry))
;;; From Rosettacode ;;;
(def *input*)
 
(def *output*)
 
(defstruct data :ptr :cells)
 
(defn inc-ptr [next-cmd]
  (fn [data]
    (next-cmd (assoc data :ptr (inc (:ptr data))))))
 
(defn dec-ptr [next-cmd]
  (fn [data]
    (next-cmd (assoc data :ptr (dec (:ptr data))))))
 
(defn inc-cell [next-cmd]
  (fn [data]
    (let [ptr (:ptr data)
          cells (:cells data)]
      (next-cmd (assoc data :cells (assoc cells ptr (inc (get cells ptr 0))))))))
 
(defn dec-cell [next-cmd]
  (fn [data]
    (let [ptr (:ptr data)
          cells (:cells data)]
      (next-cmd (assoc data :cells (assoc cells ptr (dec (get cells ptr 0))))))))
 
(defn output-cell [next-cmd]
  (fn [data]
    (set! *output* (conj *output* (get (:cells data) (:ptr data) 0)))
    (next-cmd data)))
 
(defn input-cell [next-cmd]
  (fn [data]
    (let [[input & rest-input] *input*]
      (set! *input* rest-input)
      (next-cmd (assoc data :cells (assoc (:cells data) (:ptr data) input))))))
 
(defn if-loop [next-cmd loop-cmd]
  (fn [data]
    (next-cmd (loop [d data]
                (if (zero? (get (:cells d) (:ptr d) 0))
                  d
                  (recur (loop-cmd d)))))))
 
(defn terminate [data] data)
 
(defn split-cmds [cmds]
  (letfn [(split [[cmd & rest-cmds] loop-cmds]
                 (when (nil? cmd) (throw (Exception. "invalid commands: missing ]")))
                 (condp = cmd
		   \[ (let [[c l] (split-cmds rest-cmds)]
			(split c (str loop-cmds "[" l "]")))
		   \] [(apply str rest-cmds) loop-cmds]
		   (split rest-cmds (str loop-cmds cmd))))]
    (split cmds "")))
 
(defn compile-cmds [[cmd & rest-cmds]]
  (if (nil? cmd)
    terminate
    (condp = cmd
          \> (inc-ptr (compile-cmds rest-cmds))
          \< (dec-ptr (compile-cmds rest-cmds))
          \+ (inc-cell (compile-cmds rest-cmds))
          \- (dec-cell (compile-cmds rest-cmds))
          \. (output-cell (compile-cmds rest-cmds))
          \, (input-cell (compile-cmds rest-cmds))
          \[ (let [[cmds loop-cmds] (split-cmds rest-cmds)]
               (if-loop (compile-cmds cmds) (compile-cmds loop-cmds)))
          \] (throw (Exception. "invalid commands: missing ["))
          (compile-cmds rest-cmds))))
 
(defn compile-and-run [cmds input]
  (binding [*input* input *output* []]
    (let [compiled-cmds (compile-cmds cmds)]
      (println (compiled-cmds (struct data 0 {}))))
    (println *output*)
    (println (apply str (map char *output*)))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defplugin
  (:cmd
   "Executes brainfuck."
   #{"bf"} 
   (fn [{:keys [args] :as com-m}]
     (let [[bf & input] args]
       (doseq [x (-> bf 
                     (compile-and-run input) 
                     with-out-str 
                     (#(.split % "\n")))]
         (send-message com-m x))))))