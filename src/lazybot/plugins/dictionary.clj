(ns lazybot.plugins.dictionary
  (:require [lazybot.registry :as registry]
            [lazybot.utilities :refer [prefix]]
            [wordnik.api.word :as wword]
            [wordnik.api.words :as wwords]
            [clojure.string :as string]))

(registry/defplugin 
  (:cmd
   "Takes a word and look's up it's definition via the Wordnik dictionary API." 
   #{"dict"} 
   (fn [{:keys [bot channel user-nick args] :as com-m}]
     (registry/send-message
      com-m 
      (prefix user-nick
              (if-let [the-word (first args)]
                (let [key (get-in @bot [:config :dictionary :wordnik-key])
                      definition (first (wword/definitions the-word :api_key key))
                      text (:text definition)]
                  (if (seq text)
                    (str (:partOfSpeech definition) ": " text)
                    "Word not found."))
                "You didn't give me a word. Duh.")))))
  
  (:cmd
   "Wordnik's Word Of The Day"
   #{"wotd"}
   (fn [{:keys [bot channel nick args] :as com-m}]
     (registry/send-message
      com-m
      (prefix nick
              (let [key (get-in @bot [:config :dictionary :wordnik-key])
                    wotd (wwords/wotd :api_key key)
                    definition (:text (first (:definitions wotd)))]
                (if (seq wotd)
                  (str (:word wotd) ": " definition)
                  "No word of the day today, sorry!"))))))

  (:cmd
   "Common bi-gram phrases for the given word"
   #{"phrases"}
   (fn [{:keys [bot channel nick args] :as com-m}]
     (registry/send-message
      com-m
      (prefix nick
              (if-let [the-word (first args)]
                (let [key (get-in @bot [:config :dictionary :wordnik-key])
                      phrases (wword/phrases (first args) :api_key key)]
                  (if (seq phrases)
                    (->> (wword/phrases (first args) :api_key key)
                         (map (juxt :gram1 :gram2))
                         (map (partial string/join " "))
                         (string/join ", "))
                    (str "No phrases found for " (first args) ".")))
                "I can't show you phrases if you don't give me a word."))))))

   
