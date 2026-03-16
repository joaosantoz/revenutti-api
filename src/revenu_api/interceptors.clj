(ns revenu-api.interceptors
  (:require [io.pedestal.interceptor :as interceptor]))

(defn db-interceptor [db]
  (interceptor/interceptor
    {:name  ::db
     :enter (fn [ctx]
              (assoc-in ctx [:request :db] db))}))
