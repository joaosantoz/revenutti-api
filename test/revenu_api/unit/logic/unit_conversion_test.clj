(ns revenu-api.unit.logic.unit-conversion-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.logic.unit-conversion :as unit-conversion]))

(deftest factor-test
  (testing "weight units"
    (is (= 1    (unit-conversion/factor "g")))
    (is (= 1000 (unit-conversion/factor "kg"))))
  (testing "volume units"
    (is (= 1    (unit-conversion/factor "ml")))
    (is (= 1000 (unit-conversion/factor "l"))))
  (testing "count units"
    (is (= 1  (unit-conversion/factor "unit")))
    (is (= 12 (unit-conversion/factor "dozen")))))

(deftest base-unit-test
  (is (= "g"    (unit-conversion/base-unit "g")))
  (is (= "g"    (unit-conversion/base-unit "kg")))
  (is (= "ml"   (unit-conversion/base-unit "ml")))
  (is (= "ml"   (unit-conversion/base-unit "l")))
  (is (= "unit" (unit-conversion/base-unit "unit")))
  (is (= "unit" (unit-conversion/base-unit "dozen"))))

(deftest compatible?-test
  (testing "weight base accepts weight units"
    (is (unit-conversion/compatible? "g" "g"))
    (is (unit-conversion/compatible? "g" "kg")))
  (testing "weight base rejects non-weight units"
    (is (not (unit-conversion/compatible? "g" "ml")))
    (is (not (unit-conversion/compatible? "g" "l")))
    (is (not (unit-conversion/compatible? "g" "unit")))
    (is (not (unit-conversion/compatible? "g" "dozen"))))
  (testing "volume base accepts volume units"
    (is (unit-conversion/compatible? "ml" "ml"))
    (is (unit-conversion/compatible? "ml" "l")))
  (testing "count base accepts count units"
    (is (unit-conversion/compatible? "unit" "unit"))
    (is (unit-conversion/compatible? "unit" "dozen"))))

(deftest accepted-units-test
  (is (= #{"g" "kg"}       (unit-conversion/accepted-units "g")))
  (is (= #{"ml" "l"}       (unit-conversion/accepted-units "ml")))
  (is (= #{"unit" "dozen"} (unit-conversion/accepted-units "unit"))))
