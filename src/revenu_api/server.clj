(ns revenu-api.server
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [revenu-api.config :as config]
            [revenu-api.interceptors :as interceptors]
            [revenu-api.infra.http-server :as http-server]))

(defn- service-config [db]
  {::http/routes          http-server/routes
   ::http/type            :jetty
   ::http/port            (config/port)
   ::http/host            "0.0.0.0"
   ::http/join?           false
   ::http/allowed-origins {:creds true
                           :allowed-origins #{config/allowed-origin}}})

(defrecord Server [db server]
  component/Lifecycle
  (start [component]
    (let [server (-> (service-config db)
                     http/default-interceptors
                     (update ::http/interceptors into [(body-params/body-params)
                                                       http/json-body
                                                       (interceptors/db-interceptor db)])
                     http/create-server
                     http/start)]
      (println (str "Server running on port " (config/port)))
      (assoc component :server server)))

  (stop [component]
    (when server
      (http/stop server))
    (assoc component :server nil)))

(defn new-server []
  (map->Server {}))
