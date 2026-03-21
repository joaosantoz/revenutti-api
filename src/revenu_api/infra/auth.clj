(ns revenu-api.infra.auth
  (:require [buddy.sign.jwt :as jwt]
            [revenu-api.config :as config])
  (:import [java.time Instant]))

(defn sign-access-token
  [user-id]
  (jwt/sign {:sub (str user-id)
             :exp (+ (.getEpochSecond (Instant/now)) 900)}
            config/jwt-secret
            {:alg :hs256}))

(defn sign-refresh-token
  [user-id]
  (jwt/sign {:sub  (str user-id)
             :type "refresh"
             :exp  (+ (.getEpochSecond (Instant/now)) 604800)}
            config/jwt-secret
            {:alg :hs256}))

(defn verify-token
  [token]
  (jwt/unsign token config/jwt-secret {:alg :hs256}))
