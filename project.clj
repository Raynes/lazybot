(defproject lazybot "0.7.0-alpha1"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.reader "0.7.3"]
                 [commons-lang/commons-lang "2.5"]
                 [commons-io/commons-io "1.4"]
                 [backtype/clj-time "0.3.2"]
                 [clojail "1.0.6"]
                 [clj-http "0.7.7"]
                 [irclj "0.4.1"]
                 [congomongo "0.4.0"]
                 [clj-config "0.2.0"]
                 [compojure "1.1.5"]
                 [ring/ring-jetty-adapter "1.2.0-beta1"]
                 [log4j "1.2.16"]
                 [org.thnetos/cd-client "0.3.3"]
                 [org.jruby/jruby "1.6.5"]
                 [clj-wordnik "0.1.0-alpha1"]
                 [cheshire "5.0.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/data.zip "0.1.0"]
                 [org.clojure/tools.cli "0.1.0"]
                 [useful "0.8.4"]
                 [hobbit "0.1.0-alpha1"]
                 [ororo "0.1.0"]
                 [socrates "0.0.1"]
                 [innuendo "0.1.7"]
                 [frinj "0.1.2"]
                 [tentacles "0.2.1"]
                 [findfn "0.1.3"]
                 [me.raynes/laser "0.1.16"]
                 [hiccup "1.0.2"]
                 [me.raynes/least "0.1.3"]]
  :uberjar-name "lazybot.jar"
  :main lazybot.run
  :copy-deps true
  :resources-path "resource")
