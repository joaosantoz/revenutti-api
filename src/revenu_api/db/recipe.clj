(ns revenu-api.db.recipe
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import [java.util UUID]))

(defn insert!
  [database user-id recipe]
  (let [query {:insert-into :recipes
               :values      [{:user_id           user-id
                              :name              (:name recipe)
                              :yield_portions    (:yield_portions recipe)
                              :target_margin_bps (:target_margin_bps recipe)}]
               :returning   [:id :name :yield_portions :target_margin_bps :is_active :created_at :updated_at]}]
    (db/query-one database (sql/format query))))

(defn get-all!
  [database user-id]
  (let [query {:select [:id :name :yield_portions :target_margin_bps :is_active :created_at :updated_at]
               :from   [:recipes]
               :where  [:and
                        [:= :recipes.is_active true]
                        [:= :recipes.user_id   user-id]]}]
    (db/query database (sql/format query))))

(defn find!
  [database user-id id]
  (let [query {:select [:id :name :yield_portions :target_margin_bps :is_active :created_at :updated_at]
               :from   [:recipes]
               :where  [:and
                        [:= :recipes.id        (UUID/fromString id)]
                        [:= :recipes.is_active true]
                        [:= :recipes.user_id   user-id]]}]
    (db/query-one database (sql/format query))))

(defn update!
  [database user-id id recipe]
  (let [query {:update    :recipes
               :set       (cond-> {:updated_at [:now]}
                            (:name recipe)              (assoc :name (:name recipe))
                            (:yield_portions recipe)    (assoc :yield_portions (:yield_portions recipe))
                            (:target_margin_bps recipe) (assoc :target_margin_bps (:target_margin_bps recipe)))
               :where     [:and
                           [:= :recipes.id        (UUID/fromString id)]
                           [:= :recipes.is_active true]
                           [:= :recipes.user_id   user-id]]
               :returning [:id :name :yield_portions :target_margin_bps :is_active :created_at :updated_at]}]
    (db/query-one database (sql/format query))))

(defn deactivate!
  [database user-id id]
  (let [query {:update    :recipes
               :set       {:is_active  false
                           :updated_at [:now]}
               :where     [:and
                           [:= :recipes.id        (UUID/fromString id)]
                           [:= :recipes.is_active true]
                           [:= :recipes.user_id   user-id]]
               :returning [:id]}]
    (db/query-one database (sql/format query))))
