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
    (is (unit-conversion/compatible? "unit" "dozen")))
  (testing "unit accepted for ml base when unit-size provided"
    (is (unit-conversion/compatible? "ml" "unit" 900)))
  (testing "unit accepted for g base when unit-size provided"
    (is (unit-conversion/compatible? "g" "unit" 500)))
  (testing "unit rejected for ml base without unit-size"
    (is (not (unit-conversion/compatible? "ml" "unit"))))
  (testing "unit rejected for unit base even with unit-size"
    (is (not (unit-conversion/compatible? "unit" "ml" 900)))))

(deftest accepted-units-test
  (is (= #{"g" "kg"}       (unit-conversion/accepted-units "g")))
  (is (= #{"ml" "l"}       (unit-conversion/accepted-units "ml")))
  (is (= #{"unit" "dozen"} (unit-conversion/accepted-units "unit")))
  (testing "unit added to accepted units for ml base when unit-size provided"
    (is (= #{"ml" "l" "unit"} (unit-conversion/accepted-units "ml" 900))))
  (testing "unit added to accepted units for g base when unit-size provided"
    (is (= #{"g" "kg" "unit"} (unit-conversion/accepted-units "g" 500))))
  (testing "unit base unchanged even with unit-size"
    (is (= #{"unit" "dozen"} (unit-conversion/accepted-units "unit" 12)))))
