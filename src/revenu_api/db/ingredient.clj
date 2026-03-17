(ns revenu-api.db.ingredient
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import (java.util UUID)))

(defn insert!
  [database ingredient]
  (let [query {:insert-into :ingredients
               :values      [{:name    (:name ingredient)
                              :unit    (name (:unit ingredient))
                              :user_id #uuid "1d5c42d5-0432-47c3-abc2-82045e7f40b8"}]
               :returning   [:id :name :unit :created_at]}]
    (db/query-one database (sql/format query))))

(defn get-all!
  [database]
  (let [query {:select [:id :user_id :name :unit :created_at :updated_at]
               :from   [:ingredients]
               :where  [:= :ingredients.is_active true]}]
    (db/query database (sql/format query))))

(defn update!
  [database id ingredient]
  (let [query {:update    :ingredients
               :set       (cond-> {:updated_at [:now]}
                                  (:name ingredient) (assoc :name (:name ingredient))
                                  (:unit ingredient) (assoc :unit (:unit ingredient)))
               :where     [:and
                           [:= :ingredients.id (UUID/fromString id)]
                           [:= :ingredients.is_active true]]
               :returning [:id :name :unit :created_at :updated_at]}]
    (db/query-one database (sql/format query))))

(defn find!
  [database id]
  (let [uuid  (UUID/fromString id)
        query {:select [:id :user_id :name :unit :created_at :updated_at]
               :from   [:ingredients]
               :where  [:and
                        [:= :ingredients.id        uuid]
                        [:= :ingredients.is_active true]]}]
    (db/query-one database (sql/format query))))

(defn deactivate!
  [database id]
  (let [uuid  (UUID/fromString id)
        query {:update    :ingredients
               :set       {:is_active  false
                           :updated_at [:now]}
               :where     [:and
                           [:= :ingredients.id        uuid]
                           [:= :ingredients.is_active true]]
               :returning [:id]}]
    (db/query-one database (sql/format query))))
