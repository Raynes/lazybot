(let [chat {"irc.freenode.net" ["#depd"]}
      de-repo "https://github.com/DirectEmployers/"
      repos ["direct-seo" "lazybot" "saved-search" "DotJobs-MetaProfiles"
             "django-my-urls" "myjobs" "sendgrid-smtp-api" "UI-Framework"]]
  {:servers ["irc.freenode.net"]
   :prepends #{"@"}
   :weather {:token ""}
   :dictionary {:wordnik-key
                "99c266291da87b231f40a0c8902040da0b568588c25526cff"} 
   :sed {:automatic? true}
   ;; The maximum number of operations that can be running at any given time.   
   :max-operations 3
   ;; The number of operations running right now
   :pending-ops 0
   :prefix-arrow "\u21D2 "
   :help {:admin-add? true  ; only admins can add help topics
          :admin-rm? true}   ; only admins can remove help topics
   :clojure {:eval-prefixes {:defaults ["->" "." "," ; prefixes in any channel
                                        ;; recognize multi-char prefixes
                                        ;; starting with `&`
                                        #"&\|(.*?)(?=\|&|\|&|$)"
                                        ;; multi-char prefixes starting with
                                        ;; `##`
                                        #"##(([^#]|#(?!#))+)\s*((##)?(?=.*##)|$)"]
                             }}
   :servers-port 21310
   :github {:commits (into {} (for [x repos] [(str de-repo x) chat]))}
   "irc.freenode.net" (read-string (slurp
                                    (str (System/getProperty "user.dir")
                                         "/.lazybot/secret.clj")))})

; users is a series of username to password and privileges.
; plugins is a list of plugins to load at startup.
