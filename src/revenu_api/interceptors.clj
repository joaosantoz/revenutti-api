(ns revenu-api.interceptors
  (:require [io.pedestal.interceptor :as interceptor]
            [revenu-api.logic.auth :as auth])
  (:import [java.util UUID]))

(defn db-interceptor [db]
  (interceptor/interceptor
    {:name  ::db
     :enter (fn [ctx]
              (assoc-in ctx [:request :db] db))}))

(def jwt-interceptor
  (interceptor/interceptor
    {:name  ::jwt
     :enter (fn [ctx]
              (let [token (some-> (get-in ctx [:request :headers "authorization"])
                                  (clojure.string/replace #"(?i)^bearer " ""))]
                (if-not token
                  (assoc ctx :response {:status 401
                                        :body   {:title  "Unauthorized"
                                                 :status 401
                                                 :detail "Missing Authorization header"}})
                  (try
                    (let [claims (auth/verify-token token)]
                      (assoc-in ctx [:request :user-id] (UUID/fromString (:sub claims))))
                    (catch Exception _
                      (assoc ctx :response {:status 401
                                            :body   {:title  "Unauthorized"
                                                     :status 401
                                                     :detail "Invalid or expired token"}}))))))}))
