{:channels ["#()"] ; A list of channels to connect to.
 :prepend \$ ; The character you want for a prepend. Currently set to $
 :server "irc.freenode.net"
 :bitly-login "" ; Your bit.ly login.
 :bitly-key "" ; API key and login above needed for URL shortening.
 :wordnik-key "" ; API key needed for dictionary access.
 :bot-password ""
 :users {"JohnDoe" {:pass "iliekpie", :privs :admin}
	 "JaneDoe" {:pass "ohai", :privs :admin}}}

; users is a series of username password combinations.