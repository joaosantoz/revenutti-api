(ns revenu-api.server
  (:require [io.pedestal.http :as http]
            [revenu-api.config :as config]
            [revenu-api.db.core :as db]))

(defn- service-config []
  {::http/routes #{}
   ::http/type   :jetty
   ::http/port   config/port
   ::http/join?  false})

(defonce ^:private server (atom nil))

(defn start []
  (db/start)
  (try
    (db/query-one "SELECT 1")
    (println "DB connection OK")
    (catch Exception e
      (println "DB connection FAILED:" (.getMessage e))))
  (reset! server (-> (service-config) http/create-server http/start))
  (println (str "Server running on port " config/port)))

(defn stop []
  (when @server
    (http/stop @server)
    (reset! server nil))
  (db/stop))
