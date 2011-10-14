(ns lazybot.plugins.translate
  (:use [lazybot registry utilities]
        [clojure.data.json :only [read-json]])
  (:require [clojure-http.resourcefully :as res]
            [clojure.string :as s])
  (:import org.apache.commons.lang.StringEscapeUtils))

(defn translate [lang1 lang2 text]
  (-> (res/get 
       "http://ajax.googleapis.com/ajax/services/language/translate"
       {} {"v" "1.0" "q" text "langpair" (str lang1 "|" lang2)})
      :body-seq first read-json))

(defplugin
  (:cmd
   "Translates with google translate. Takes two language
   abbreviations (google's ones) and some text to translate, and
   returns it translated."  #{"trans" "translate"}
   (fn [{:keys [bot channel args] :as com-m}]
     (let [[lang-from lang-to & text] args
           translation (translate lang-from lang-to (s/join " " text))]
       (if (:responseData translation)
         (send-message com-m (-> translation 
                                 :responseData 
                                 :translatedText 
                                 StringEscapeUtils/unescapeHtml
                                 (.replaceAll "\n|\r" "")))
         (send-message com-m "Languages not recognized."))))))