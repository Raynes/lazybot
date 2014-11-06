(ns lazybot.plugins.operator
  (:require [lazybot.registry :as registry]
            [clojure.string :as string]
            [lazybot.plugins.login :refer [when-privs]]
            [irclj.core :as ircb]))

(defn build-command
  [mode args]
  (str mode \space (string/join \space args)))

(registry/defplugin
  (:cmd
   "Sets the person you specify as operator. ADMIN ONLY."
   #{"op"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/mode com channel (build-command "+o" args)))))

  (:cmd
   "Deops the person you specify. ADMIN ONLY."
   #{"deop"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/mode com channel (build-command "-o" args)))))

  (:cmd
   "Kicks the person you specify. ADMIN ONLY."
   #{"kick"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/kick com channel (first args) (string/join \space (rest args))))))

  #_
  (:cmd
   "Set's the channel's topic. ADMIN ONLY."
   #{"settopic"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/topic com channel (string/join \space args)))))

  (:cmd
   "Ban's whatever you specify. ADMIN ONLY."
   #{"ban"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/mode com channel (build-command "+b" args)))))

  (:cmd
   "Unban whatever you specify. ADMIN ONLY."
   #{"unban"}
   (fn [{:keys [com bot nick channel args] :as com-m}]
     (when-privs com-m :admin (ircb/mode com channel (build-command "-b" args)))))

  (:cmd
   "Voices the person you specify. ADMIN 0NLY."
   #{"voice"}
   (fn [{:keys [com bot channel nick args] :as com-m}]
     (when-privs com-m :admin (ircb/mode com channel (build-command "+v" args)))))

  (:cmd
   "Devoices the person you specify. ADMIN ONLY."
   #{"devoice"}
   (fn [{:keys [com bot channel nick args] :as com-m}]
     (when-privs com-m :admin (ircb/mode com channel (build-command "-v" args))))))
