(ns revenu-api.controllers.auth
  (:require [revenu-api.adapters.user :as adapter]
            [revenu-api.db.user :as db]
            [revenu-api.db.refresh-token :as refresh-token.db]
            [revenu-api.infra.google :as google]
            [revenu-api.infra.auth :as auth])
  (:import [java.util UUID]))

(defn- issue-tokens!
  [database user-id]
  (let [refresh-token (auth/sign-refresh-token user-id)]
    (refresh-token.db/insert! database user-id
                              (auth/hash-token refresh-token)
                              (auth/refresh-token-expires-at))
    {:access-token  (auth/sign-access-token user-id)
     :refresh-token refresh-token}))

(defn google-callback!
  [database code]
  (let [google-tokens (google/exchange-code-for-tokens code)
        google-user   (google/fetch-user-info (:access_token google-tokens))
        user          (-> (db/upsert-user! database {:external-provider "google"
                                                     :external-id       (:sub google-user)
                                                     :name              (:name google-user)
                                                     :email             (:email google-user)
                                                     :avatar-url        (:picture google-user)})
                          (adapter/db->internal))]
    (issue-tokens! database (:id user))))

(defn refresh-tokens!
  [database token]
  (let [claims  (try (auth/verify-token token)
                     (catch Exception _
                       (throw (ex-info "Invalid or expired token" {:status 401}))))
        user-id (UUID/fromString (:sub claims))]
    (when-not (= "refresh" (:type claims))
      (throw (ex-info "Invalid token type" {:status 401})))
    (when-not (refresh-token.db/find-by-hash! database (auth/hash-token token))
      (throw (ex-info "Token not found or already revoked" {:status 401})))
    (refresh-token.db/delete-by-hash! database (auth/hash-token token))
    (issue-tokens! database user-id)))

(defn logout!
  [database token]
  (refresh-token.db/delete-by-hash! database (auth/hash-token token)))

(defn find-user!
  [database user-id]
  (when-let [row (db/find-by-id! database user-id)]
    (let [user (adapter/db->internal row)]
      (cond-> {:id    (:id user)
               :name  (:name user)
               :email (:email user)}
        (contains? user :avatar-url) (assoc :avatar_url (:avatar-url user))))))
