{:channels ["#()"] ; A list of channels to connect to.
 :prepend \$ ; The character you want for a prepend. Currently set to $
 :server "irc.freenode.net"
 :bitly-login "" ; Your bit.ly login.
 :bitly-key "" ; API key and login above needed for URL shortening.
 :wordnik-key "" ; API key needed for dictionary access.
 :bot-password ""
 :users {"JohnDoe" {:pass "iliekpie", :privs :admin}
	 "JaneDoe" {:pass "ohai", :privs :admin}}
 :plugins [;"dictionary"
	   "lmgtfy"
	   "google"
	   "translate"
	   "8ball"
	   "utils"
	   "leet"
	   "eval"
	   "login"
	   "weather"
	   "brainfuck"
	   "whatis"
	   ;"shorturl"
	   "spellcheck"
	   "dynamic"]}

; users is a series of username to password and privileges.
; plugins is a list of plugins to load at startup.