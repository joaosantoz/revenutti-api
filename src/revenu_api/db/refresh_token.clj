(ns revenu-api.db.refresh-token
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import [java.util UUID]))

(defn insert!
  [database user-id token-hash expires-at]
  (let [query {:insert-into :refresh_tokens
               :values      [{:user_id    user-id
                              :token_hash token-hash
                              :expires_at (java.sql.Timestamp/from expires-at)}]
               :returning   [:id]}]
    (db/query-one database (sql/format query))))

(defn find-by-hash!
  [database token-hash]
  (let [query {:select [:id :user_id]
               :from   [:refresh_tokens]
               :where  [:and
                        [:= :refresh_tokens.token_hash token-hash]
                        [:> :refresh_tokens.expires_at [:now]]]}]
    (db/query-one database (sql/format query))))

(defn delete-by-hash!
  [database token-hash]
  (let [query {:delete-from :refresh_tokens
               :where       [:= :refresh_tokens.token_hash token-hash]}]
    (db/query-one database (sql/format query))))
