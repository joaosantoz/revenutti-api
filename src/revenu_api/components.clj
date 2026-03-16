(ns revenu-api.components
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [revenu-api.db.core :as db]
            [revenu-api.server :as server]))

(defn create-system []
  (component/system-map
    :db     (db/new-database)
    :server (component/using (server/new-server) [:db])))

(defn bootstrap! []
  (component/start (create-system)))