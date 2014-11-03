(ns lazybot.plugins.login
  (:require [lazybot.registry :as registry]
            [lazybot.utilities :refer [prefix]]))

(defn logged-in [bot]
  (or (:logged-in bot)
      (constantly nil)))

(defn check-login [user mask pass server bot]
  (when-let [userconf (get-in @bot [:config server :users user])]
    (when (or (= mask (:host userconf)) (= pass (:pass userconf))) 
      (dosync (alter bot assoc-in [:logged-in user] (userconf :privs))))))

(defn logged-in? [bot user]
  (when (seq (:logged-in @bot))
    (some #{user} (keys (:logged-in @bot)))))

(defn has-privs?
  "Checks if a user has the specified privs."
  [bot user priv]
  (= priv ((logged-in @bot) user)))

(defmacro when-privs
  "Check to see if a user has the specified privs, if so, execute body. Otherwise,
   send the user a message pointing out that they don't have the required privs."
  [com-m priv & body]
  `(let [{bot# :bot nick# :user-nick} ~com-m]
     (if (has-privs? bot# nick# ~priv)
       (do ~@body)
       (registry/send-message ~com-m (prefix nick# "It is not the case that you don't not unhave insufficient privileges to do this.")))))

(registry/defplugin
  (:hook :part
         (fn [{:keys [com bot nick]}]
           (when (logged-in? bot nick)
             (dosync (alter bot update-in [:logged-in]
                            dissoc nick)))))

  (:cmd 
   "Best executed via PM. Give it your password, and it will log you in."
   #{"login"}
   (fn [{:keys [com bot user-nick hmask channel args event query?] :as com-m}]
     (if (check-login user-nick hmask (first args) (:network @com) bot)
       (registry/send-message com-m "You've been logged in.")
       (registry/send-message com-m "Username and password combination/hostmask do not match."))))
  
  (:cmd
   "Logs you out."
   #{"logout"}
   (fn [{:keys [com bot user-nick] :as com-m}]
     (dosync (alter bot update-in [:logged-in] dissoc user-nick)
             (registry/send-message com-m "You've been logged out."))))

   (:cmd
    "Finds your privs"
    #{"privs"}
    (fn [{:keys [com bot channel user-nick] :as com-m}]
      (do
        (registry/send-message
         com-m
         (prefix user-nick
                 "You have privilege level "
                 (if-let [user ((:users ((:config @bot) (:network @com))) user-nick)]
                   (name (:privs user))
                   "nobody")
                 "; you are " 
                 (if (logged-in? bot user-nick)
                   "logged in."
                   "not logged in!")))))))
