(let [plugins #{#_"dictionary" "lmgtfy" "google" "translate" "eball" "utils" "leet" "clojure" "login" "log"
                "weather" "brainfuck" "whatis" "shorturl" "haskell"
                "mail" "timer" "fortune" "rss" "title" "operator" "seen" "sed" "help"
                "load" "embedded" "karma" "markov" "yesno" "autoreply"}]
  {:servers ["irc.freenode.net"]        ; A list of servers.
   :prepends #{"@"}   ; The character you want for a prepend. Currently set to @
   :bitly-login ""    ; Your bit.ly login.
   :bitly-key ""      ; API key and login above needed for URL shortening.
   :dont-sed #{"tomoj" "hiredman"}
   :wordnik-key ""                      ; API key needed for dictionary access.
   :max-operations 3 ; The maximum number of operations that can be running at any given time.
   :pending-ops 0    ; The number of operations running right now
   :admin-add? true  ; only admins can add help topics
   :admin-rm? true   ; only admins can remove help topics
   :eval-prefixes {:defaults ["->" "." "," ; prefixes in any channel
                              #"&\|(.*?)(?=\|&|\|&|$)" ; stuff like &|this|&
                              #"##(([^#]|#(?!#))+)\s*((##)?(?=.*##)|$)"]
                   ;; list of prefixes NOT to use in certain channels
                   "#tempchan" ["->"]   ; turn this off for testing
                   "#clojure" [","]}    ; let clojurebot have this one
   :servers-port 8080                  ; port for plugins that require webserver
   "irc.freenode.net" {:channels ["#tempchan"]
                       :bot-name "lazybot-test"
                       :bot-password nil
                       :users {"JohnDoe" {:pass "iliekpie", :privs :admin}
                               "JaneDoe" {:pass "ohai", :privs :admin}}
                       :user-blacklist #{"Meowzorz"}
                       :autoreplies {"#clojure" {#".*(https?://)richhickey(.github.com/\S*).*" "Nooooo, that's so out of date! Please see instead $1clojure$2 and try to stop linking to rich's repo."}}
                       :catch-links? {true} ; Should only be enabled if the title plugin is activated below.
                       :channel-catch-blacklist #{} ; Channels in which URL title scraper is to be disabled.
                       :url-blacklist #{} ; URL title scraper will look for these words in URLs and not use them if they appear.
                       :user-ignore-url-blacklist [["bot" "ters"]] ; A series of "match this" but "not this" pairs.}
                       :plugins plugins} ; A series of "match this" but "not this" pairs.
   :plugins plugins})

; users is a series of username to password and privileges.
; plugins is a list of plugins to load at startup.
