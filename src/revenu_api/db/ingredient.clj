(ns revenu-api.db.ingredient
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db]))

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