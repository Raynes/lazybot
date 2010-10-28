{:servers ["irc.freenode.net"]          ; A list of servers.
 :prepends #{"@"} ; The character you want for a prepend. Currently set to @
 :bitly-login ""  ; Your bit.ly login.
 :bitly-key ""	  ; API key and login above needed for URL shortening.
 :wordnik-key ""  ; API key needed for dictionary access.
 :max-operations 3 ; The maximum number of operations that can be running at any given time.
 :admin-add? true  ; only admins can add help topics
 :admin-rm? true   ; only admins can remove help topics
 :eval-prefixes {:defaults ["->" "." ","] ; prefixes in any channel
                 ;; list of prefixes NOT to use in certain channels
                 "#tempchan" ["->"]       ; turn this off for testing
                 "#clojure" [","]}        ; let clojurebot have this one
 :servers-port 8080    ; port for plugins that require webserver
 "irc.freenode.net" {:channels ["#tempchan"]
		     :bot-name "sexpbot-test"
		     :bot-password nil
		     :users {"JohnDoe" {:pass "iliekpie", :privs :admin}
                     "JaneDoe" {:pass "ohai", :privs :admin}}
		     :user-blacklist #{"Meowzorz"}
		     :catch-links? {true} ; Should only be enabled if the title plugin is activated below.
		     :channel-catch-blacklist #{} ; Channels in which URL title scraper is to be disabled.
		     :url-blacklist #{} ; URL title scraper will look for these words in URLs and not use them if they appear.
		     :user-ignore-url-blacklist [["bot" "ters"]] ; A series of "match this" but "not this" pairs.}
		     :plugins #{#_"dictionary" "lmgtfy" "google" "translate" "eball" "utils" "leet" "eval" "login"
				"weather" "brainfuck" "whatis" "shorturl" "dynamic" "haskell"
				"mail" "timer" "fortune" "rss" "title" "operator" "seen" "sed" "help" "status"
                                "load" "embedded" "karma"}} ; A series of "match this" but "not this" pairs.
 :plugins #{#_"dictionary" "lmgtfy" "google" "translate" "eball" "utils" "leet" "eval" "login"
	    "weather" "brainfuck" "whatis" "shorturl" "dynamic" "haskell"
	    "mail" "timer" "fortune" "rss" "title" "operator" "seen" "sed" "help" "status"
            "load" "embedded" "karma"}}

; users is a series of username to password and privileges.
; plugins is a list of plugins to load at startup.
