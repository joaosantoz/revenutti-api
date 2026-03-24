(ns revenu-api.unit.logic.recipe-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.logic.recipe :as logic.recipe]))

(def ^:private flour
  {:id               "ingredient-uuid"
   :name             "Flour"
   :usage-quantity   500
   :usage-unit       "g"
   :price-cents      500
   :package-quantity 1000
   :package-unit     "g"})

(deftest recipe-pricing-test
  (testing "recipe with no ingredients returns zero totals"
    (let [result (logic.recipe/recipe-pricing {:servings 4 :target-margin-bps 5000} [])]
      (is (zero? (:total-cost result)))
      (is (zero? (:cost-per-portion result)))
      (is (zero? (:suggested-price-per-portion result)))
      (is (zero? (:suggested-price-total result)))
      (is (= [] (:ingredients result)))))

  (testing "unpriced ingredient sets all cost fields to nil"
    (let [unpriced (assoc flour :price-cents nil)
          result   (logic.recipe/recipe-pricing {:servings 2 :target-margin-bps 3000} [unpriced])]
      (is (nil? (:total-cost result)))
      (is (nil? (:cost-per-portion result)))
      (is (nil? (:suggested-price-per-portion result)))
      (is (nil? (:suggested-price-total result)))))

  (testing "mix of priced and unpriced sets all cost fields to nil"
    (let [unpriced (assoc flour :price-cents nil)
          result   (logic.recipe/recipe-pricing {:servings 2 :target-margin-bps 3000} [flour unpriced])]
      (is (nil? (:total-cost result)))))

  (testing "zero price-cents is treated as priced (not nil)"
    (let [free   (assoc flour :price-cents 0)
          result (logic.recipe/recipe-pricing {:servings 1 :target-margin-bps 5000} [free])]
      (is (some? (:total-cost result)))
      (is (= 0M (:total-cost result)))))

  (testing "calculates correct totals for single ingredient"
    ;; 500g flour from 1000g package at 500 cents
    ;; cost-per-gram = 500 / 1000 = 0.5 cents
    ;; ingredient-cost = 0.5 * 500 = 250 cents
    ;; cost-per-portion (2 servings) = 125 cents
    ;; suggested-price-per-portion (50% markup) = 125 * 1.5 = 187.5 cents
    ;; suggested-price-total = 187.5 * 2 = 375 cents
    (let [result (logic.recipe/recipe-pricing {:servings 2 :target-margin-bps 5000} [flour])]
      (is (= 250M  (:total-cost result)))
      (is (= 125M  (:cost-per-portion result)))
      (is (= 187.5M (:suggested-price-per-portion result)))
      (is (= 375M  (:suggested-price-total result)))))

  (testing "ingredient cost is attached to each ingredient in result"
    (let [result     (logic.recipe/recipe-pricing {:servings 2 :target-margin-bps 5000} [flour])
          ingredient (first (:ingredients result))]
      (is (= 250M (:cost ingredient)))))

  (testing "unit conversion applied for kg package and g usage"
    ;; 500g from 1kg package (1000g) at 1000 cents
    ;; cost-per-gram = 1000 / 1000 = 1 cent
    ;; ingredient-cost = 1 * 500 = 500 cents
    (let [ingredient {:id "uuid" :name "Sugar"
                      :usage-quantity 500 :usage-unit "g"
                      :price-cents 1000 :package-quantity 1 :package-unit "kg"}
          result     (logic.recipe/recipe-pricing {:servings 1 :target-margin-bps 0} [ingredient])]
      (is (= 500M (:total-cost result)))))

  (testing "unit-size used as package factor when package-unit is unit"
    ;; 100ml oil from 1 bottle (900ml) at 900 cents
    ;; cost-per-ml = 900 / (1 * 900) = 1 cent/ml
    ;; ingredient-cost = 1 * 100 = 100 cents
    (let [ingredient {:id "uuid" :name "Soybean Oil"
                      :usage-quantity 100 :usage-unit "ml"
                      :unit-size 900
                      :price-cents 900 :package-quantity 1 :package-unit "unit"}
          result     (logic.recipe/recipe-pricing {:servings 1 :target-margin-bps 0} [ingredient])]
      (is (= 100M (:total-cost result))))))

(deftest actual-margin-test
  (testing "100% markup when selling for double the cost"
    (is (= 100M (logic.recipe/actual-margin 2000M 1000M))))
  (testing "50% markup"
    (is (= 50M (logic.recipe/actual-margin 1500M 1000M))))
  (testing "zero cost returns nil"
    (is (nil? (logic.recipe/actual-margin 1000M 0M))))
  (testing "nil cost returns nil"
    (is (nil? (logic.recipe/actual-margin 1000M nil)))))
