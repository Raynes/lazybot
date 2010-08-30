(ns sexpbot.plugins.login
  (:use [sexpbot respond]))

(defn check-pass-login [user pass server bot]
  (let [userconf ((:users ((:config @bot) server)) user)]
    (when (= pass (:pass userconf)) 
      (dosync (alter bot assoc-in [:logged-in server user] (userconf :privs))))))

(defn logged-in? [server bot user]
  (when (seq (:logged-in @bot))
    (some #{user} (keys ((:logged-in @bot) server)))))

(defplugin
  (:add-hook :on-quit
	     (fn [{:keys [irc bot nick]}]
               (when (logged-in? (:server @irc) bot nick)
                 (dosync (alter bot update-in [:logged-in (:server @irc)]
                                dissoc nick)))))

  (:login 
   "Best executed via PM. Give it your password, and it will log you in."
   ["login"] 
   [{:keys [irc bot nick channel args]}]
   (if (check-pass-login nick (first args) (:server @irc) bot)
     (send-message irc bot channel "You've been logged in.")
     (send-message irc bot channel "Username and password combination do not match.")))
  
  (:logout 
   "Logs you out."
   ["logout"] 
   [{:keys [irc bot nick channel]}]
   (dosync (alter bot update-in [:logged-in (:server @irc)] dissoc nick)
	   (send-message irc bot channel "You've been logged out.")))

   (:privs
   "Finds your privs"
   ["privs"]
   [{:keys [irc bot channel nick]}]
   (do
     (send-message irc bot channel 
                   (str nick ": You are a"
                        (if (not= :admin (:privs ((:users ((:config @bot) (:server @irc))) nick)))
                          " regular user."
                          (str "n admin; you are " 
                               (if (logged-in? (:server @irc) bot nick) "logged in." "not logged in!"))))))))