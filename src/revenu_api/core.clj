(ns revenu-api.core
  (:require [io.pedestal.http :as http])
  (:gen-class))

(defn greet
  [request]
  {:status 200
   :body "Hello, World!"})

(def routes #{["/greet" :get `greet]})

(def service
  {::http/routes   routes
   ::http/type     :jetty
   ::http/port     8080
   ::http/join?    false})

(defonce server (atom nil))

(defn start []
  (reset! server (-> service http/create-server http/start)))

(defn stop []
  (when @server
    (http/stop @server)
    (reset! server nil)))

(defn -main [& _args]
  (start)
  (println "Server running on port 8080"))
