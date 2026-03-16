(defproject revenu-api "0.1.0-SNAPSHOT"
  :description "Revenu API backend"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 ;; HTTP
                 [io.pedestal/pedestal.service "0.6.4"]
                 [io.pedestal/pedestal.jetty   "0.6.4"]
                 ;; Database
                 [com.github.seancorfield/next.jdbc    "1.3.909"]
                 [com.github.seancorfield/honeysql      "2.6.1203"]
                 [org.postgresql/postgresql             "42.7.3"]
                 [com.zaxxer/HikariCP                   "5.1.0"]
                 ;; Component
                 [com.stuartsierra/component        "1.1.0"]
                 ;; Migrations
                 [ragtime "0.8.0"]
                 ;; Validation
                 [prismatic/schema "1.4.1"]
                 ;; JSON
                 [cheshire "5.12.0"]
                 ;; Config
                 [environ "1.2.0"]
                 ;; Logging
                 [ch.qos.logback/logback-classic "1.4.12"]
                 [org.slf4j/slf4j-api            "2.0.9"]
                 [org.slf4j/jul-to-slf4j         "2.0.9"]]
  :main ^:skip-aot revenu-api.core
  :target-path "target/%s"
  :plugins [[lein-environ "1.2.0"]]
  :aliases {"migrate"  ["run" "-m" "revenu-api.db.migration/migrate"]
            "rollback" ["run" "-m" "revenu-api.db.migration/rollback"]}
  :profiles {:dev        {:source-paths ["dev"]
                         :dependencies [[org.clojure/tools.namespace "1.4.4"]]}
             :uberjar   {:aot :all
                         :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :repl-start {:injections   [(require '[revenu-api.components :as components])
                                         (components/bootstrap!)]
                          :repl-options {:init-ns user}}})
