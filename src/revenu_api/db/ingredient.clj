(ns revenu-api.db.ingredient
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import (java.util UUID)))

(defn insert!
  [database user-id ingredient]
  (let [query {:insert-into :ingredients
               :values      [{:name    (:name ingredient)
                              :unit    (name (:unit ingredient))
                              :user_id user-id}]
               :returning   [:id :name :unit :created_at]}]
    (db/query-one database (sql/format query))))

(defn get-all!
  [database user-id]
  (let [query {:select [:id :user_id :name :unit :created_at :updated_at]
               :from   [:ingredients]
               :where  [:and
                        [:= :ingredients.is_active true]
                        [:= :ingredients.user_id  user-id]]}]
    (db/query database (sql/format query))))

(defn update!
  [database user-id id ingredient]
  (let [query {:update    :ingredients
               :set       (cond-> {:updated_at [:now]}
                            (:name ingredient) (assoc :name (:name ingredient))
                            (:unit ingredient) (assoc :unit (:unit ingredient)))
               :where     [:and
                           [:= :ingredients.id      (UUID/fromString id)]
                           [:= :ingredients.is_active true]
                           [:= :ingredients.user_id  user-id]]
               :returning [:id :name :unit :created_at :updated_at]}]
    (db/query-one database (sql/format query))))

(defn find!
  [database user-id id]
  (let [query {:select [:id :user_id :name :unit :created_at :updated_at]
               :from   [:ingredients]
               :where  [:and
                        [:= :ingredients.id        (UUID/fromString id)]
                        [:= :ingredients.is_active true]
                        [:= :ingredients.user_id   user-id]]}]
    (db/query-one database (sql/format query))))

(defn deactivate!
  [database user-id id]
  (let [query {:update    :ingredients
               :set       {:is_active  false
                           :updated_at [:now]}
               :where     [:and
                           [:= :ingredients.id        (UUID/fromString id)]
                           [:= :ingredients.is_active true]
                           [:= :ingredients.user_id   user-id]]
               :returning [:id]}]
    (db/query-one database (sql/format query))))
