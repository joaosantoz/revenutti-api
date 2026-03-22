(ns revenu-api.db.price-history
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import [java.util UUID]))

(defn insert!
  [database ingredient-id price-history]
  (let [query {:insert-into :price_history
               :values      [{:ingredient_id (UUID/fromString ingredient-id)
                              :price_cents   (:price-cents price-history)
                              :package_quantity  (:package-quantity price-history)
                              :package_unit  (:package-unit price-history)
                              :purchase_date [:cast (:purchase-date price-history) :date]}]
               :returning   [:id :ingredient_id :price_cents :package_quantity :package_unit :purchase_date]}]
    (db/query-one database (sql/format query))))

(defn get-all!
  [database user-id ingredient-id]
  (let [query {:select    [:price_history.*]
               :from      [:price_history]
               :join      [:ingredients [:= :price_history.ingredient_id :ingredients.id]]
               :where     [:and
                           [:= :price_history.ingredient_id (UUID/fromString ingredient-id)]
                           [:= :ingredients.user_id         user-id]
                           [:= :ingredients.is_active       true]]
               :order-by  [[:price_history.purchase_date :desc] [:price_history.id :desc]]}]
    (db/query database (sql/format query))))
