(ns lazybot.plugins.operator
  (:use lazybot.registry
        [lazybot.plugins.login :only [when-privs]])
  (:require [irclj.core :as ircb]))

(defplugin
  (:cmd
   "Sets the person you specify as operator. ADMIN ONLY."
   #{"op"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/set-mode com channel "+o" (first args)))))

  (:cmd
   "Deops the person you specify. ADMIN ONLY."
   #{"deop"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/set-mode com channel  "-o" (first args)))))

  (:cmd
   "Kicks the person you specify. ADMIN ONLY."
   #{"kick"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/kick com channel (first args) :reason (apply str (rest args))))))

  (:cmd
   "Set's the channel's topic. ADMIN ONLY."
   #{"settopic"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/set-topic com channel (apply str (interpose " " args))))))

  (:cmd
   "Ban's whatever you specify. ADMIN ONLY."
   #{"ban"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/set-mode com channel "+b" (first args)))))

  (:cmd
   "Unban whatever you specify. ADMIN ONLY."
   #{"unban"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/set-mode com channel "-b" (first args)))))

  (:cmd
   "Voices the person you specify. ADMIN 0NLY."
   #{"voice"}
   (fn [{:keys [com bot channel nick args] :as com-m}]
     (when-privs com-m :admin (ircb/set-mode com channel "+v" (first args)))))

  (:cmd
   "Devoices the person you specify. ADMIN ONLY."
   #{"devoice"}
   (fn [{:keys [com bot channel nick args] :as com-m}]
     (when-privs com-m :admin (ircb/set-mode com channel "-v" (first args))))))
