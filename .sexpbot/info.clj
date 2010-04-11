{:servers ["irc.freenode.net"]	    ; A list of servers.
 :channels {"irc.freenode.net" ["#()"]} ; A map of server names to lists of channels to connect to.
 :prepend \$ ; The character you want for a prepend. Currently set to $
 :bitly-login ""  ; Your bit.ly login.
 :bitly-key ""	  ; API key and login above needed for URL shortening.
 :wordnik-key ""  ; API key needed for dictionary access.
 :bot-name {"irc.freenode.net" "sexpbot"} ; You get the point.
 :bot-password {} ; A map of server to bot password
 :users {"JohnDoe" {:pass "iliekpie", :privs :admin}
	 "JaneDoe" {:pass "ohai", :privs :admin}}
 :plugins [;"dictionary"
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
	   ;"shorturl"
	   "spellcheck"
	   "dynamic"
	   "walton"
	   "haskell"
	   "mail"
	   "timer"
       "fortune"]}

; users is a series of username to password and privileges.
; plugins is a list of plugins to load at startup.
