(ns revenu-api.db.ingredient
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import (java.util UUID)))

(defn insert!
  [database user-id ingredient]
  (let [query {:insert-into :ingredients
               :values      [(cond-> {:name      (:name ingredient)
                                      :base_unit (:base-unit ingredient)
                                      :user_id   user-id}
                               (:brand ingredient)     (assoc :brand (:brand ingredient))
                               (:unit-size ingredient) (assoc :unit_size (:unit-size ingredient)))]
               :returning   [:id :name :base_unit :brand :unit_size]}]
    (db/query-one database (sql/format query))))

(defn get-all!
  [database user-id]
  (let [query {:select   [:id :user_id :name :base_unit :brand :unit_size]
               :from     [:ingredients]
               :where    [:and
                          [:= :ingredients.is_active true]
                          [:= :ingredients.user_id  user-id]]
               :order-by [[:created_at :asc]]}]
    (db/query database (sql/format query))))

(defn update!
  [database user-id id ingredient]
  (let [query {:update    :ingredients
               :set       (cond-> {:updated_at [:now]}
                            (:name ingredient)                (assoc :name (:name ingredient))
                            (contains? ingredient :brand)      (assoc :brand (:brand ingredient))
                            (contains? ingredient :unit-size) (assoc :unit_size (:unit-size ingredient)))
               :where     [:and
                           [:= :ingredients.id        (UUID/fromString id)]
                           [:= :ingredients.is_active true]
                           [:= :ingredients.user_id   user-id]]
               :returning [:id :name :base_unit :brand :unit_size]}]
    (db/query-one database (sql/format query))))

(defn find!
  [database user-id id]
  (let [query {:select [:id :user_id :name :base_unit :brand :unit_size]
               :from   [:ingredients]
               :where  [:and
                        [:= :ingredients.id        (UUID/fromString id)]
                        [:= :ingredients.is_active true]
                        [:= :ingredients.user_id   user-id]]}]
    (db/query-one database (sql/format query))))

(defn find-by-ids!
  [database user-id ids]
  (let [query {:select [:id :base_unit :unit_size]
               :from   [:ingredients]
               :where  [:and
                        [:= :ingredients.is_active true]
                        [:= :ingredients.user_id user-id]
                        [:in :ingredients.id (map #(UUID/fromString %) ids)]]}]
    (db/query database (sql/format query))))

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
