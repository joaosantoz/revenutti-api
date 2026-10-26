(defproject revenu-api "0.1.0-SNAPSHOT"
  :description "Revenu API backend"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
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
                 ;; Auth
                 [buddy/buddy-sign "3.5.351"]
                 [buddy/buddy-auth "3.0.323"]
                 ;; HTTP client
                 [clj-http "3.12.3"]
                 ;; Config
                 [environ "1.2.0"]
                 ;; Logging
                 [ch.qos.logback/logback-classic "1.4.12"]
                 [org.slf4j/slf4j-api            "2.0.9"]
                 [org.slf4j/jul-to-slf4j         "2.0.9"]]
  :main ^:skip-aot revenu-api.core
  :target-path "target/%s"
  :plugins [[lein-environ "1.2.0"]]
  :test-selectors {:unit        (fn [test-metadata] (re-find #"\.unit\." (str (:ns test-metadata))))
                  :integration (fn [test-metadata] (re-find #"\.integration\." (str (:ns test-metadata))))}
  :aliases {"migrate"     ["run" "-m" "revenu-api.db.migration/migrate"]
            "rollback"    ["run" "-m" "revenu-api.db.migration/rollback"]
            "test"        ["with-profile" "+test" "test"]
            "unit"        ["with-profile" "+test" "test" ":unit"]
            "integration" ["with-profile" "+test" "test" ":integration"]}
  :profiles {:dev        {:source-paths ["dev"]
                         :dependencies [[org.clojure/tools.namespace "1.4.4"]]}
             :uberjar   {:aot :all
                         :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :repl-start {:injections   [(require '[revenu-api.components :as components])
                                         (components/bootstrap!)]
                          :repl-options {:init-ns user
                                         :port    7888
                                         :host    "0.0.0.0"}}
             :test      {:env {:app-env     "test"
                               :port        "3000"
                               :db-url      "jdbc:postgresql://localhost:5432/revenu_test"
                               :db-user     "revenu"
                               :db-password "secret"
                               :jwt-secret  "test-secret-32-chars-exactly!!!"}}})
