(ns revenu-api.controllers.auth
  (:require [revenu-api.adapters.user :as adapter]
            [revenu-api.db.user :as db]
            [revenu-api.infra.google :as google]
            [revenu-api.infra.auth :as auth])
  (:import [java.util UUID]))

(defn google-callback!
  [database code]
  (let [google-tokens (google/exchange-code-for-tokens code)
        google-user   (google/fetch-user-info (:access_token google-tokens))
        user          (-> (db/upsert-user! database {:external-provider "google"
                                                     :external-id       (:sub google-user)
                                                     :name              (:name google-user)
                                                     :email             (:email google-user)})
                          (adapter/db->internal))]
    {:access-token  (auth/sign-access-token (:id user))
     :refresh-token (auth/sign-refresh-token (:id user))}))

(defn refresh-tokens!
  [database token]
  (let [claims  (auth/verify-token token)
        _       (when-not (= "refresh" (:type claims))
                  (throw (ex-info "Invalid token type" {:status 401})))
        user-id (UUID/fromString (:sub claims))]
    (when-not (db/find-by-id! database user-id)
      (throw (ex-info "User not found" {:status 401})))
    {:access-token  (auth/sign-access-token user-id)
     :refresh-token (auth/sign-refresh-token user-id)}))
