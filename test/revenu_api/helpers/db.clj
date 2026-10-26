(ns revenu-api.helpers.db
  (:require [clojure.string :as str]
            [environ.core :refer [env]]
            [next.jdbc :as jdbc]
            [revenu-api.db.core :as db.core]
            [revenu-api.db.migration :as migration])
  (:import [java.sql DriverManager]))

(defn- postgres-admin-url
  []
  (str/replace (env :db-url) #"/[^/]+$" "/postgres"))

(defn- test-db-name
  []
  (last (str/split (env :db-url) #"/")))

(defn- ensure-db-exists!
  []
  (let [connection (DriverManager/getConnection (postgres-admin-url) (env :db-user) (env :db-password))]
    (try
      (when-not (jdbc/execute-one! connection ["SELECT 1 FROM pg_database WHERE datname = ?" (test-db-name)])
        (jdbc/execute! connection [(str "CREATE DATABASE " (test-db-name))]))
      (finally
        (.close connection)))))

(defonce ^:private test-db-setup
  (delay
    (ensure-db-exists!)
    (migration/migrate)))

(defn open-test-connection
  []
  @test-db-setup
  (doto (DriverManager/getConnection (env :db-url) (env :db-user) (env :db-password))
    (.setAutoCommit false)))

(defmacro with-rollback
  [[db-binding] & body]
  `(let [connection# (open-test-connection)
         ~db-binding  (db.core/map->Database {:connection connection#})]
     (try
       ~@body
       (finally
         (.rollback connection#)
         (.close connection#)))))
