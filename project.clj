(defproject revenu-api "0.1.0-SNAPSHOT"
  :description "Revenu API backend"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 ;; HTTP
                 [io.pedestal/pedestal.service "0.6.4"]
                 [io.pedestal/pedestal.jetty   "0.6.4"]
                 ;; Validation
                 [prismatic/schema "1.4.1"]
                 ;; JSON
                 [cheshire "5.12.0"]
                 ;; Config
                 [environ "1.2.0"]
                 ;; Logging
                 [ch.qos.logback/logback-classic "1.4.12"]
                 [org.slf4j/jul-to-slf4j         "2.0.9"]]
  :main ^:skip-aot revenu-api.core
  :target-path "target/%s"
  :plugins [[lein-environ "1.2.0"]]
  :profiles {:dev     {:env {:app-env "development"}}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
