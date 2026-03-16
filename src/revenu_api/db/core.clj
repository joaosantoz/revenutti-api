(ns revenu-api.db.core
  (:require [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as jdbc-conn]
            [revenu-api.config :as config])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defrecord Database [connection]
  component/Lifecycle

  (start [component]
    (let [conn (jdbc-conn/->pool HikariDataSource
                               {:jdbcUrl  config/db-url
                                :username config/db-user
                                :password config/db-password})]
      (println "Starting database")
      (assoc component :connection conn)))

  (stop [component]
    (when connection
      (println "Stopping database")
      (.close ^HikariDataSource connection))
    (assoc component :connection nil)))

(defn new-database
  []
  (map->Database {}))

(defn query
  [database sql]
  (jdbc/execute! (:connection database) sql))

(defn query-one
  [database sql]
  (jdbc/execute-one! (:connection database) sql))

