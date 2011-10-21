(ns lazybot.plugins.dictionary
  (:use lazybot.registry
        [lazybot.utilities :only [prefix]]
        clj-wordnik.core))

(defplugin 
  (:cmd
   "Takes a word and look's up it's definition via the Wordnik dictionary API." 
   #{"dict"} 
   (fn [{:keys [bot channel nick args]:as com-m}]
     (send-message
      com-m 
      (prefix nick 
              (let [definition (first
                                (definitions (:wordnik-key (:config @bot))
                                  (first args)))
                    text (:text definition)]
                (if (seq text)
                  (str (:partOfSpeech definition) ": " text)
                  "Word not found.")))))))