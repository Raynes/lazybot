{:servers ["irc.freenode.net"]	    ; A list of servers.
 :channels {"irc.freenode.net" ["#tempchan"]} ; A map of server names to lists of channels to connect to.
 :prepend \@ ; The character you want for a prepend. Currently set to @
 :bitly-login ""  ; Your bit.ly login.
 :bitly-key ""	  ; API key and login above needed for URL shortening.
 :wordnik-key ""  ; API key needed for dictionary access.
 :bot-name {"irc.freenode.net" "sexpbot-test"} ; You get the point.
 :bot-password {}; ; A map of server to bot password
 :max-operations 3 ; The maximum number of operations that can be running at any given time.
 :users {"irc.freenode.net"
	 {"JohnDoe" {:pass "iliekpie", :privs :admin}
	  "JaneDoe" {:pass "ohai", :privs :admin}}}
 :user-blacklist {"irc.freenode.net" #{"Meowzorz"}} ; ignore these users on these servers.
 :log false ; set to true if you want logs.
 :catch-links? {"irc.freenode.net" true} ; Should only be enabled if the title plugin is activated below.
 :channel-catch-blacklist {"irc.freenode.net" #{}} ; Channels in which URL title scraper is to be disabled.
 :url-blacklist #{} ; URL title scraper will look for these words in URLs and not use them if they appear.
 :user-ignore-url-blacklist {"irc.freenode.net" [["bot" "ters"]]} ; A series of "match this" but "not this" pairs.
 :admin-add? true ; only admins can add help topics
 :admin-rm? true ; only admins can remove help topics
 :plugins [;"dictionary"
	   "sed"
	   "help"
	   "lmgtfy"
	   "google"
	   "translate"
	   "eball"
	   "utils"
	   "leet"
	   "eval"
	   "login"
	   "weather"
	   "brainfuck"
	   "whatis"
	   "shorturl"
	   "spellcheck"
	   "dynamic"
	   "walton"
	   "haskell"
	   "mail"
	   "timer"
	   "fortune"
	   "rss"
	   "title"
	   "log"]}

; users is a series of username to password and privileges.
; plugins is a list of plugins to load at startup.
