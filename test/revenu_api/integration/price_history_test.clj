(ns revenu-api.integration.price-history-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.controllers.price-history :as controllers.price-history]
            [revenu-api.helpers.db :refer [with-rollback]]
            [revenu-api.helpers.factories :as factories]))

(deftest add-price-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))
          result     (controllers.price-history/add-price! db (:id user) (str (:id ingredient))
                                                           {:price-cents      999
                                                            :package-quantity 1000
                                                            :package-unit     "g"
                                                            :purchase-date    "2024-06-01"})]
      (is (uuid? (:id result)))
      (is (= 999  (:price-cents result)))
      (is (= "g"  (:package-unit result))))))

(deftest add-price-unknown-ingredient-test
  (with-rollback [db]
    (let [user (factories/make-user! db)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not found"
            (controllers.price-history/add-price! db (:id user) (str (java.util.UUID/randomUUID))
                                                  {:price-cents      500
                                                   :package-quantity 1000
                                                   :package-unit     "g"
                                                   :purchase-date    "2024-01-01"}))))))

(deftest add-price-other-users-ingredient-test
  (with-rollback [db]
    (let [user-a     (factories/make-user! db)
          user-b     (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user-b))]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not found"
            (controllers.price-history/add-price! db (:id user-a) (str (:id ingredient))
                                                  {:price-cents      500
                                                   :package-quantity 1000
                                                   :package-unit     "g"
                                                   :purchase-date    "2024-01-01"}))))))

(deftest add-price-unit-package-with-unit-size-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user) {:name "Soybean Oil" :base-unit "ml" :unit-size 900})
          result     (controllers.price-history/add-price! db (:id user) (str (:id ingredient))
                                                           {:price-cents      2000
                                                            :package-quantity 10
                                                            :package-unit     "unit"
                                                            :purchase-date    "2024-06-01"})]
      (is (= "unit" (:package-unit result)))
      (is (= 10M     (:package-quantity result))))))

(deftest add-price-unit-package-rejected-without-unit-size-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user) {:name "Soybean Oil" :base-unit "ml"})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid package_unit"
            (controllers.price-history/add-price! db (:id user) (str (:id ingredient))
                                                  {:price-cents      2000
                                                   :package-quantity 10
                                                   :package-unit     "unit"
                                                   :purchase-date    "2024-06-01"}))))))

(deftest get-prices-ordered-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))
          price-jan  (factories/make-price! db (:id ingredient) {:purchase-date "2024-01-01"})
          price-jun  (factories/make-price! db (:id ingredient) {:purchase-date "2024-06-01"})
          price-mar  (factories/make-price! db (:id ingredient) {:purchase-date "2024-03-01"})
          prices     (controllers.price-history/get-prices! db (:id user) (str (:id ingredient)))]
      (is (= 3 (count prices)))
      (is (= "2024-06-01" (subs (:purchase-date (first prices)) 0 10))))))
