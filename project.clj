(defproject clj-wamp "2.0.0-SNAPSHOT"
  :description "The WebSocket Application Messaging Protocol for Clojure"
  :url "https://github.com/cgmartin/clj-wamp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.incubator "0.1.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/data.codec "0.1.0"]
                 [http-kit "2.1.19"]
                 [cheshire "5.2.0"]
                 [org.clojure/tools.cli "0.3.3"]  ; dev only for using /example/main.clj
                 [compojure "1.4.0"]  ; dev only for using /example/main.clj
                 [stylefruits/gniazdo "0.4.0"]]
  :source-paths ["src"]
  :profiles {:1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"] ; https://github.com/ring-clojure/ring/issues/100
                                  [log4j "1.2.17" :exclusions [javax.mail/mail
                                                               javax.jms/jms
                                                               com.sun.jdmk/jmxtools
                                                               com.sun.jmx/jmxri]]]}})
