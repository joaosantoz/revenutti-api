(ns revenu-api.infra.google
  (:require [clj-http.client :as http]
            [revenu-api.config :as config]))

(defn exchange-code-for-tokens
  [code]
  (-> (http/post "https://oauth2.googleapis.com/token"
                 {:form-params {:code          code
                                :client_id     config/google-client-id
                                :client_secret config/google-client-secret
                                :redirect_uri  config/google-redirect-uri
                                :grant_type    "authorization_code"}
                  :as          :json})
      :body))

(defn fetch-user-info
  [google-access-token]
  (-> (http/get "https://www.googleapis.com/oauth2/v3/userinfo"
                {:headers {"Authorization" (str "Bearer " google-access-token)}
                 :as      :json})
      :body))
