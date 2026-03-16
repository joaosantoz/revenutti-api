(ns revenu-api.db.migration
  (:require [ragtime.jdbc :as ragtime-jdbc]
            [ragtime.repl :as ragtime]
            [revenu-api.config :as config]))

(defn- db-url []
  (str config/db-url "?user=" config/db-user "&password=" config/db-password))

(defn- ragtime-config []
  {:datastore  (ragtime-jdbc/sql-database (db-url))
   :migrations (ragtime-jdbc/load-resources "migrations")})

(defn migrate  [] (ragtime/migrate  (ragtime-config)))
(defn rollback [] (ragtime/rollback (ragtime-config)))
