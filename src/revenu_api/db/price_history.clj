(ns revenu-api.db.price-history
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import [java.util UUID]))

(defn insert!
  [database ingredient-id price-history]
  (let [query {:insert-into :price_history
               :values      [{:ingredient_id (UUID/fromString ingredient-id)
                              :price_cents   (:price-cents price-history)
                              :package_qty   (:package_qty price-history)
                              :purchase_date [:cast (:purchase_date price-history) :date]}]
               :returning   [:id :ingredient_id :price_cents :package_qty :purchase_date]}]
    (db/query-one database (sql/format query))))

(defn get-all!
  [database user-id ingredient-id]
  (let [query {:select    [[:ph.id :id]
                            [:ph.ingredient_id :ingredient_id]
                            [:ph.price_cents :price_cents]
                            [:ph.package_qty :package_qty]
                            [:ph.purchase_date :purchase_date]]
               :from      [[:price_history :ph]]
               :join      [[:ingredients :i] [:= :ph.ingredient_id :i.id]]
               :where     [:and
                           [:= :ph.ingredient_id (UUID/fromString ingredient-id)]
                           [:= :i.user_id        user-id]
                           [:= :i.is_active       true]]
               :order-by  [[:ph.purchase_date :desc] [:ph.id :desc]]}]
    (db/query database (sql/format query))))
