(ns revenu-api.unit.adapters.ingredient-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.adapters.ingredient :as adapter])
  (:import [java.util UUID]))

(deftest wire->internal-test
  (testing "maps name and base_unit without brand"
    (is (= {:name "Flour" :base-unit "g"}
           (adapter/wire->internal {:name "Flour" :base_unit "g"}))))
  (testing "includes brand when present"
    (is (= {:name "Flour" :base-unit "g" :brand "Bunge"}
           (adapter/wire->internal {:name "Flour" :base_unit "g" :brand "Bunge"})))))

(deftest db->internal-test
  (let [id (UUID/randomUUID)]
    (testing "maps all namespaced keys to internal"
      (is (= {:id id :name "Flour" :base-unit "g" :is-active true :brand "Bunge"}
             (adapter/db->internal {:ingredients/id        id
                                    :ingredients/name      "Flour"
                                    :ingredients/base_unit "g"
                                    :ingredients/is_active true
                                    :ingredients/brand     "Bunge"}))))
    (testing "brand is nil when absent from row"
      (is (nil? (:brand (adapter/db->internal {:ingredients/id        id
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
  (testing "both name and brand"
    (is (= {:name "Sugar" :brand "Bunge"}
           (adapter/update-wire->internal {:name "Sugar" :brand "Bunge"})))))

(deftest internal->wire-test
  (let [id (UUID/randomUUID)]
    (testing "brand key absent when nil"
      (let [result (adapter/internal->wire {:id id :name "Flour" :base-unit "g" :brand nil :is-active true})]
        (is (= {:id id :name "Flour" :base_unit "g"} result))
        (is (not (contains? result :brand)))))
    (testing "brand present when set"
      (is (= {:id id :name "Flour" :base_unit "g" :brand "Bunge"}
             (adapter/internal->wire {:id id :name "Flour" :base-unit "g" :brand "Bunge" :is-active true}))))))
