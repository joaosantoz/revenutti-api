(ns revenu-api.db.core
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as jdbc-conn]
            [revenu-api.config :as config])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defonce ^:private datasource (atom nil))

(def ^:dynamic *conn* nil)

(defn- conn []
  (or *conn* @datasource))

(defn start []
  (reset! datasource
          (jdbc-conn/->pool HikariDataSource
                            {:jdbcUrl  config/db-url
                             :username config/db-user
                             :password config/db-password})))

(defn stop []
  (when @datasource
    (.close ^HikariDataSource @datasource)
    (reset! datasource nil)))

(defn query [sql & params]
  (jdbc/execute! (conn) (into [sql] params)))

(defn query-one [sql & params]
  (jdbc/execute-one! (conn) (into [sql] params)))

(defmacro with-transaction [& body]
  `(jdbc/with-transaction [tx# @datasource]
     (binding [*conn* tx#]
       ~@body)))
