(ns revenu-api.unit.adapters.recipe-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.adapters.recipe :as adapter])
  (:import [java.util UUID]
           [org.postgresql.util PGobject]))

(defn- make-pg-object
  [json-str]
  (doto (PGobject.)
    (.setValue json-str)))

(deftest wire->internal-test
  (testing "converts target_margin percentage to basis points"
    (is (= 5000 (:target-margin-bps (adapter/wire->internal {:name "Cake" :servings 8 :target_margin 50.0 :ingredients []})))))
  (testing "preserves fractional margin without float errors"
    (is (= 9999 (:target-margin-bps (adapter/wire->internal {:name "Cake" :servings 8 :target_margin 99.99 :ingredients []})))))
  (testing "passes name, servings and ingredients through"
    (let [result (adapter/wire->internal {:name "Cake" :servings 8 :target_margin 50.0 :ingredients [{:id "abc" :usage_quantity 1 :usage_unit "g"}]})]
      (is (= "Cake" (:name result)))
      (is (= 8      (:servings result)))
      (is (= 1      (count (:ingredients result)))))))

(deftest db->internal-test
  (let [id (UUID/randomUUID)]
    (testing "nil ingredients returns empty vector"
      (let [result (adapter/db->internal {:recipes/id                id
                                          :recipes/name              "Cake"
                                          :recipes/servings          4
                                          :recipes/target_margin_bps 5000
                                          :recipes/is_active         true
                                          :ingredients               nil})]
        (is (= [] (:ingredients result)))))

    (testing "LEFT JOIN null row is filtered out"
      (let [pg-object (make-pg-object "[{\"id\":null,\"usage_quantity\":null,\"usage_unit\":null,\"name\":null,\"base_unit\":null,\"brand\":null}]")
            result    (adapter/db->internal {:recipes/id                id
                                             :recipes/name              "Cake"
                                             :recipes/servings          4
                                             :recipes/target_margin_bps 5000
                                             :recipes/is_active         true
                                             :ingredients               pg-object})]
        (is (= [] (:ingredients result)))))

    (testing "valid ingredients are parsed from JSON"
      (let [pg-object (make-pg-object "[{\"id\":\"abc\",\"usage_quantity\":1.5,\"usage_unit\":\"kg\",\"name\":\"Flour\",\"base_unit\":\"g\",\"brand\":null}]")
            result    (adapter/db->internal {:recipes/id                id
                                             :recipes/name              "Cake"
                                             :recipes/servings          4
                                             :recipes/target_margin_bps 5000
                                             :recipes/is_active         true
                                             :ingredients               pg-object})
            ingredient (first (:ingredients result))]
        (is (= 1             (count (:ingredients result))))
        (is (= "abc"         (:id ingredient)))
        (is (= 1.5           (:usage_quantity ingredient)))
        (is (= "kg"          (:usage_unit ingredient)))
        (is (not (contains? ingredient :brand)))))

    (testing "maps scalar fields"
      (let [result (adapter/db->internal {:recipes/id                id
                                          :recipes/name              "Cake"
                                          :recipes/servings          4
                                          :recipes/target_margin_bps 5000
                                          :recipes/is_active         true
                                          :ingredients               nil})]
        (is (= id     (:id result)))
        (is (= "Cake" (:name result)))
        (is (= 4      (:servings result)))
        (is (= 5000   (:target-margin-bps result)))
        (is (= true   (:is-active result)))))))

(deftest update-wire->internal-test
  (testing "empty map returns empty map"
    (is (= {} (adapter/update-wire->internal {}))))
  (testing "only name"
    (is (= {:name "Pudding"} (adapter/update-wire->internal {:name "Pudding"}))))
  (testing "target_margin converted to basis points"
    (is (= {:target-margin-bps 3000} (adapter/update-wire->internal {:target_margin 30.0}))))
  (testing "all optional keys present"
    (let [result (adapter/update-wire->internal {:name "Pudding" :servings 6 :target_margin 40.0})]
      (is (= "Pudding" (:name result)))
      (is (= 6         (:servings result)))
      (is (= 4000      (:target-margin-bps result))))))

(deftest internal->wire-test
  (let [id (UUID/randomUUID)]
    (testing "converts basis points to double percentage"
      (let [result (adapter/internal->wire {:id id :name "Cake" :servings 4 :target-margin-bps 5000 :is-active true :ingredients []})]
        (is (= 50.0 (:target_margin result)))))
    (testing "9999 bps becomes 99.99"
      (let [result (adapter/internal->wire {:id id :name "Cake" :servings 4 :target-margin-bps 9999 :is-active true :ingredients []})]
        (is (= 99.99 (:target_margin result)))))))

(deftest pricing->wire-test
  (let [id     (UUID/randomUUID)
        recipe {:id id :name "Cake" :servings 4 :target-margin-bps 5000 :is-active true}]

    (testing "nil cost fields pass through as nil"
      (let [result (adapter/pricing->wire recipe
                                          {:total-cost nil :cost-per-portion nil
                                           :suggested-price-per-portion nil :suggested-price-total nil
                                           :ingredients []}
                                          nil)]
        (is (nil? (:total_cost result)))
        (is (nil? (:cost_per_portion result)))
        (is (not (contains? result :actual_margin)))))

    (testing "costs in cents are converted to 2-decimal BigDecimal"
      (let [result (adapter/pricing->wire recipe
                                          {:total-cost 999M :cost-per-portion 249.75M
                                           :suggested-price-per-portion 374.625M :suggested-price-total 1498.5M
                                           :ingredients []}
                                          nil)]
        (is (= 9.99M   (:total_cost result)))
        (is (= 2.50M   (:cost_per_portion result)))
        (is (= 3.75M   (:suggested_price_per_portion result)))
        (is (= 14.99M  (:suggested_price_total result)))))

    (testing "actual_margin present when provided"
      (let [result (adapter/pricing->wire recipe
                                          {:total-cost nil :cost-per-portion nil
                                           :suggested-price-per-portion nil :suggested-price-total nil
                                           :ingredients []}
                                          50.0M)]
        (is (contains? result :actual_margin))
        (is (= 50.0 (:actual_margin result)))))

    (testing "ingredient brand omitted when nil"
      (let [ingredient {:id "abc" :name "Flour" :usage-quantity 1.5 :usage-unit "kg"
                        :price-cents 999 :package-quantity 1 :package-unit "kg" :cost 250M}
            result     (adapter/pricing->wire recipe
                                              {:total-cost 250M :cost-per-portion 62.5M
                                               :suggested-price-per-portion 93.75M :suggested-price-total 375M
                                               :ingredients [ingredient]}
                                              nil)
            wire-ingredient (first (:ingredients result))]
        (is (not (contains? wire-ingredient :brand)))
        (is (= 2.50M (:cost wire-ingredient)))))))
