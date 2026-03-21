(ns revenu-api.infra.auth
  (:require [buddy.sign.jwt :as jwt]
            [revenu-api.config :as config])
  (:import [java.time Instant]))

(def ^:private access-token-ttl  900)    ;; 15 minutes
(def ^:private refresh-token-ttl 604800) ;; 7 days

(defn sign-access-token
  [user-id]
  (jwt/sign {:sub (str user-id)
             :exp (+ (.getEpochSecond (Instant/now)) access-token-ttl)}
            config/jwt-secret
            {:alg :hs256}))

(defn sign-refresh-token
  [user-id]
  (jwt/sign {:sub  (str user-id)
             :type "refresh"
             :exp  (+ (.getEpochSecond (Instant/now)) refresh-token-ttl)}
            config/jwt-secret
            {:alg :hs256}))

(defn verify-token
  [token]
  (jwt/unsign token config/jwt-secret {:alg :hs256}))
