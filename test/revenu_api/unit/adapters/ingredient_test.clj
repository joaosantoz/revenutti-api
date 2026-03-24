(ns revenu-api.unit.adapters.ingredient-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.adapters.ingredient :as adapter])
  (:import [java.util UUID]))

(deftest wire->internal-test
  (testing "maps name and base_unit without brand or unit_size"
    (is (= {:name "Flour" :base-unit "g"}
           (adapter/wire->internal {:name "Flour" :base_unit "g"}))))
  (testing "includes brand when present"
    (is (= {:name "Flour" :base-unit "g" :brand "Bunge"}
           (adapter/wire->internal {:name "Flour" :base_unit "g" :brand "Bunge"}))))
  (testing "includes unit_size when present"
    (is (= {:name "Oil" :base-unit "ml" :unit-size 900}
           (adapter/wire->internal {:name "Oil" :base_unit "ml" :unit_size 900}))))
  (testing "unit_size absent when not provided"
    (let [result (adapter/wire->internal {:name "Oil" :base_unit "ml"})]
      (is (not (contains? result :unit-size))))))

(deftest db->internal-test
  (let [id (UUID/randomUUID)]
    (testing "maps all namespaced keys to internal"
      (is (= {:id id :name "Flour" :base-unit "g" :is-active true :brand "Bunge" :unit-size nil}
             (adapter/db->internal {:ingredients/id        id
                                    :ingredients/name      "Flour"
                                    :ingredients/base_unit "g"
                                    :ingredients/is_active true
                                    :ingredients/brand     "Bunge"}))))
    (testing "unit_size mapped when present"
      (is (= 900 (:unit-size (adapter/db->internal {:ingredients/id        id
                                                    :ingredients/name      "Oil"
                                                    :ingredients/base_unit "ml"
                                                    :ingredients/is_active true
                                                    :ingredients/unit_size 900})))))
    (testing "unit_size is nil when absent from row"
      (is (nil? (:unit-size (adapter/db->internal {:ingredients/id        id
                                                   :ingredients/name      "Flour"
                                                   :ingredients/base_unit "g"
                                                   :ingredients/is_active true})))))))

(deftest update-wire->internal-test
  (testing "empty map returns empty map"
    (is (= {} (adapter/update-wire->internal {}))))
  (testing "only name"
    (is (= {:name "Sugar"} (adapter/update-wire->internal {:name "Sugar"}))))
  (testing "only brand"
    (is (= {:brand "Bunge"} (adapter/update-wire->internal {:brand "Bunge"}))))
  (testing "unit_size included when present"
    (is (= {:unit-size 900} (adapter/update-wire->internal {:unit_size 900}))))
  (testing "unit_size nil when explicitly set to nil"
    (let [result (adapter/update-wire->internal {:unit_size nil})]
      (is (contains? result :unit-size))
      (is (nil? (:unit-size result)))))
  (testing "unit_size absent when not in input"
    (is (not (contains? (adapter/update-wire->internal {:name "Sugar"}) :unit-size)))))

(deftest internal->wire-test
  (let [id (UUID/randomUUID)]
    (testing "brand and unit_size absent when nil"
      (let [result (adapter/internal->wire {:id id :name "Flour" :base-unit "g" :brand nil :is-active true})]
        (is (= {:id id :name "Flour" :base_unit "g"} result))
        (is (not (contains? result :brand)))
        (is (not (contains? result :unit_size)))))
    (testing "brand present when set"
      (is (= {:id id :name "Flour" :base_unit "g" :brand "Bunge"}
             (adapter/internal->wire {:id id :name "Flour" :base-unit "g" :brand "Bunge" :is-active true}))))
    (testing "unit_size present when set"
      (is (= {:id id :name "Oil" :base_unit "ml" :unit_size 900}
             (adapter/internal->wire {:id id :name "Oil" :base-unit "ml" :unit-size 900 :is-active true}))))))
