(ns revenu-api.db.user
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db]))

(defn upsert-user!
  [database user]
  (let [query {:insert-into :users
               :values      [{:external_provider (:external-provider user)
                              :external_id       (:external-id user)
                              :name              (:name user)
                              :email             (:email user)
                              :avatar_url        (:avatar-url user)}]
               :on-conflict   [:external_provider :external_id]
               :do-update-set {:name       :EXCLUDED.name
                               :email      :EXCLUDED.email
                               :avatar_url :EXCLUDED.avatar_url
                               :updated_at [:now]}
               :returning     [:id :external_provider :external_id :name :email :avatar_url]}]
    (db/query-one database (sql/format query))))

(defn find-by-id!
  [database id]
  (let [query {:select [:id :external_provider :external_id :name :email :avatar_url]
               :from   [:users]
               :where  [:= :users.id id]}]
    (db/query-one database (sql/format query))))
