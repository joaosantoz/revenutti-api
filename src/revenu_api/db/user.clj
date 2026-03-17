(ns revenu-api.db.user
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db]))

(defn upsert-user!
  [database user]
  (let [query {:insert-into :users
               :values      [{:external_provider (:external-provider user)
                              :external_id       (:external-id user)
                              :name              (:name user)
                              :email             (:email user)}]
               :on-conflict   [:external_provider :external_id]
               :do-update-set {:name       :EXCLUDED.name
                               :email      :EXCLUDED.email
                               :updated_at [:now]}
               :returning     [:id :external_provider :external_id :name :email]}]
    (db/query-one database (sql/format query))))

(defn find-by-id!
  [database id]
  (let [query {:select [:id :external_provider :external_id :name :email]
               :from   [:users]
               :where  [:= :users.id id]}]
    (db/query-one database (sql/format query))))
