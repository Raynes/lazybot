(ns lazybot.paste
  (:require [clojure.string :as string]
            [innuendo.core :as paste]
            [lazybot.utilities :as utils])
  (:import java.io.IOException))

(defn word-wrap [str]
  (string/replace str #"(.{50,70}[])}\"]*)\s+" "$1\n"))

(def paste-note "... ")
(def default-cap 300)

(defn paste [s & [language]]
  (:url (paste/create-paste s (when language {:language language}))))

(defn trim-with-paste
  "Trims the input string to a maximum of cap characters; if any
trimming is done, then a paste will be created to hold the entire
string, and the returned string will end with a link to that
paste. Prepends paste-prefix to the paste (if applicable), but not to the
result string - use this to give the paste additional context that is
not necessary in the result."
  ([s]
     (trim-with-paste default-cap "Plain Text" "" s))
  ([opt s]
   (apply trim-with-paste
          (if (number? opt)
            [opt "Plain Text" "" s]
            [default-cap opt "" s])))
  ([cap language s]
     (trim-with-paste cap language "" s))
  ([cap language paste-prefix s]
     (utils/trim-string
      cap
      (fn [s]
        (str paste-note
             (try
               (-> (str paste-prefix s) word-wrap (paste language))
               (catch IOException e
                 (str "failed to paste: " (.getMessage e))))))
      s)))