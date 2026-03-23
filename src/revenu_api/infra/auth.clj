(ns revenu-api.infra.auth
  (:require [buddy.sign.jwt :as jwt]
            [revenu-api.config :as config])
  (:import [java.security MessageDigest]
           [java.time Instant]))

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
             :jti  (str (java.util.UUID/randomUUID))
             :exp  (+ (.getEpochSecond (Instant/now)) refresh-token-ttl)}
            config/jwt-secret
            {:alg :hs256}))

(defn verify-token
  [token]
  (jwt/unsign token config/jwt-secret {:alg :hs256}))

(defn hash-token
  [token]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes token "UTF-8"))]
    (apply str (map #(format "%02x" %) digest))))

(defn refresh-token-expires-at
  []
  (Instant/ofEpochSecond (+ (.getEpochSecond (Instant/now)) refresh-token-ttl)))
