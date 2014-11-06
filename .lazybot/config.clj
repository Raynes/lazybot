(let [plugins #{"autoreply" "brainfuck" "clojure" "clojuredocs" "debug" "dictionary" "eball"
                "embedded" "fortune" "google" "haskell" "hello-world" "help" "javadoc"
                "jruby" "karma" "leet" "lmgtfy" "load" "log" "logger" "login"
                "macro" "mail" "max" "mute" "operator" "ping" "rss"
                "sed" "seen" "shorturl" "timer" "title" "unix-jokes" "utils" "weather"
                "whatis" "yesno"
                ;; "github" how does this even work?
                ;; "rotten-tomatoes", "lastfm" needs key to verify
                ;; api broken -- "knowledge" "metacritic" (getting 403 error)
                ;;               "notifo" this service went out of business
                ;;               "shorturl" is.gd is good, bit.ly seems broken
                }]
  {:servers ["irc.freenode.net"]        ; A list of servers.
   :prepends #{"@"}   ; The character you want for a prepend. Currently set to @
   :weather {:token ""} ; Wunderground token.
   :dictionary {:wordnik-key "99c266291da87b231f40a0c8902040da0b568588c25526cff"} ; Wordnik API key.
   :sed {:automatic? true}
   :max-operations 3 ; The maximum number of operations that can be running at any given time.
   :pending-ops 0    ; The number of operations running right now
   :prefix-arrow "\u21D2 "
   :help {:admin-add? true  ; only admins can add help topics
          :admin-rm? true}   ; only admins can remove help topics
   :clojure {:eval-prefixes {:defaults ["->" "." "," ; prefixes in any channel
                                        #"&\|(.*?)(?=\|&|\|&|$)" ; stuff like &|this|&
                                        #"##(([^#]|#(?!#))+)\s*((##)?(?=.*##)|$)"]
                             ;; list of prefixes NOT to use in certain channels
                             "#tempchan" ["->"]   ; turn this off for testing
                             "#clojure" [","]}}    ; let clojurebot have this one
   :servers-port 8080                  ; port for plugins that require a webserver
   "irc.freenode.net" {:channels ["#tempchan"]
                       :bot-name "lazybot-test"
                       :sed {:blacklist #{"#tempchan2"}}
                       :bot-password nil
                       :users {"JohnDoe" {:pass "iliekpie", :privs :admin}
                               "JaneDoe" {:pass "ohai", :privs :admin}}
                       :title {:blacklist #{"#foo"}}
                       :autoreply {:autoreplies {"#clojure" {#".*(https?://)richhickey(.github.com/\S*).*" "Nooooo, that's so out of date! Please see instead $1clojure$2 and try to stop linking to rich's repo."}}}
                       :plugins plugins}})

; users is a series of username to password and privileges.
; plugins is a list of plugins to load at startup.
