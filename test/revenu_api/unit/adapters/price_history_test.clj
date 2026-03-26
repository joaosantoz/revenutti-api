(ns revenu-api.unit.adapters.price-history-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.adapters.price-history :as adapter])
  (:import [java.util UUID Date]))

(deftest wire->internal-test
  (testing "converts price to integer cents"
    (is (= 999 (:price-cents (adapter/wire->internal {:price 9.99 :package_quantity 1 :package_unit "kg" :purchase_date "2024-01-01"})))))
  (testing "minimum price rounds correctly"
    (is (= 1 (:price-cents (adapter/wire->internal {:price 0.01 :package_quantity 1 :package_unit "g" :purchase_date "2024-01-01"})))))
  (testing ":price key not present in output"
    (is (not (contains? (adapter/wire->internal {:price 9.99 :package_quantity 1 :package_unit "g" :purchase_date "2024-01-01"}) :price))))
  (testing "passes package_quantity, package_unit and purchase_date through"
    (let [result (adapter/wire->internal {:price 5.00 :package_quantity 2.5 :package_unit "kg" :purchase_date "2024-06-15"})]
      (is (= 2.5         (:package-quantity result)))
      (is (= "kg"        (:package-unit result)))
      (is (= "2024-06-15" (:purchase-date result)))))
  (testing "supplier included when provided"
    (let [result (adapter/wire->internal {:price 5.00 :package_quantity 1 :package_unit "kg" :purchase_date "2024-01-01" :supplier "Atacadão"})]
      (is (= "Atacadão" (:supplier result)))))
  (testing "supplier absent when not provided"
    (let [result (adapter/wire->internal {:price 5.00 :package_quantity 1 :package_unit "kg" :purchase_date "2024-01-01"})]
      (is (not (contains? result :supplier)))))
  (testing "supplier nil when explicitly null"
    (let [result (adapter/wire->internal {:price 5.00 :package_quantity 1 :package_unit "kg" :purchase_date "2024-01-01" :supplier nil})]
      (is (nil? (:supplier result))))))

(deftest db->internal-test
  (let [id            (UUID/randomUUID)
        ingredient-id (UUID/randomUUID)
        purchase-date (Date.)]
    (testing "maps namespaced keys to kebab-case internal"
      (let [result (adapter/db->internal {:price_history/id               id
                                          :price_history/ingredient_id    ingredient-id
                                          :price_history/price_cents      999
                                          :price_history/package_quantity 1.5
                                          :price_history/package_unit     "kg"
                                          :price_history/purchase_date    purchase-date})]
        (is (= id            (:id result)))
        (is (= ingredient-id (:ingredient-id result)))
        (is (= 999           (:price-cents result)))
        (is (= 1.5           (:package-quantity result)))
        (is (= "kg"          (:package-unit result)))))
    (testing "purchase_date is converted to string"
      (let [result (adapter/db->internal {:price_history/id               id
                                          :price_history/ingredient_id    ingredient-id
                                          :price_history/price_cents      999
                                          :price_history/package_quantity 1
                                          :price_history/package_unit     "g"
                                          :price_history/purchase_date    purchase-date})]
        (is (string? (:purchase-date result)))))
    (testing "supplier mapped when present"
      (let [result (adapter/db->internal {:price_history/id               id
                                          :price_history/ingredient_id    ingredient-id
                                          :price_history/price_cents      999
                                          :price_history/package_quantity 1
                                          :price_history/package_unit     "g"
                                          :price_history/purchase_date    purchase-date
                                          :price_history/supplier         "Atacadão"})]
        (is (= "Atacadão" (:supplier result)))))
    (testing "supplier absent when not in row"
      (let [result (adapter/db->internal {:price_history/id               id
                                          :price_history/ingredient_id    ingredient-id
                                          :price_history/price_cents      999
                                          :price_history/package_quantity 1
                                          :price_history/package_unit     "g"
                                          :price_history/purchase_date    purchase-date})]
        (is (not (contains? result :supplier)))))))

(deftest internal->wire-test
  (let [id            (UUID/randomUUID)
        ingredient-id (UUID/randomUUID)
        internal      {:id              id
                       :ingredient-id   ingredient-id
                       :price-cents     999
                       :package-quantity 1
                       :package-unit    "kg"
                       :purchase-date   "2024-01-01"}]
    (testing "converts cents to 2-decimal BigDecimal"
      (is (= 9.99M (:price (adapter/internal->wire internal)))))
    (testing ":price-cents not present in output"
      (is (not (contains? (adapter/internal->wire internal) :price-cents))))
    (testing "passes package and date fields through as snake_case"
      (let [result (adapter/internal->wire internal)]
        (is (= 1          (:package_quantity result)))
        (is (= "kg"       (:package_unit result)))
        (is (= "2024-01-01" (:purchase_date result)))))
    (testing "supplier included when present"
      (let [result (adapter/internal->wire (assoc internal :supplier "Atacadão"))]
        (is (= "Atacadão" (:supplier result)))))
    (testing "supplier absent when not in internal"
      (is (not (contains? (adapter/internal->wire internal) :supplier))))))
