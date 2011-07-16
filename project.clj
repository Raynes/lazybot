(defproject lazybot "0.6.2"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [commons-lang/commons-lang "2.5"]
                 [commons-io/commons-io "1.4"]
                 [org.clojars.rayne/clj-time "0.1.0-SNAPSHOT"]
                 [clojail "0.3.4"]
                 [clojure-http-client "1.1.0-SNAPSHOT"]
                 [irclj "0.4.0-SNAPSHOT"]
                 [clojure-twitter "1.2.3"]
                 [congomongo "0.1.3-SNAPSHOT"]
                 [clj-config "0.1.0-SNAPSHOT"]
                 [clj-github "1.0.0-SNAPSHOT"]
                 [compojure "0.4.1"]
                 [ring/ring-jetty-adapter "0.2.5"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [fnparse "2.2.7"]
                 [amalloy/utils "[0.3.6,)"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :uberjar-name "lazybot"
  :main lazybot.run
  :copy-deps true
  :resources-path "resource")
