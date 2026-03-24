(ns revenu-api.integration.ingredient-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.controllers.ingredient :as controllers.ingredient]
            [revenu-api.helpers.db :refer [with-rollback]]
            [revenu-api.helpers.factories :as factories]))

(deftest create-ingredient-test
  (with-rollback [db]
    (let [user   (factories/make-user! db)
          result (controllers.ingredient/create-ingredient! db (:id user) {:name "Flour" :base-unit "g"})]
      (is (uuid? (:id result)))
      (is (= "Flour" (:name result)))
      (is (= "g"     (:base-unit result))))))

(deftest create-ingredient-with-brand-test
  (with-rollback [db]
    (let [user   (factories/make-user! db)
          result (controllers.ingredient/create-ingredient! db (:id user) {:name "Flour" :base-unit "g" :brand "Bunge"})]
      (is (= "Bunge" (:brand result))))))

(deftest get-ingredients-tenant-isolation-test
  (with-rollback [db]
    (let [user-a       (factories/make-user! db)
          user-b       (factories/make-user! db)
          ingredient-a1 (factories/make-ingredient! db (:id user-a))
          ingredient-a2 (factories/make-ingredient! db (:id user-a))
          ingredient-b  (factories/make-ingredient! db (:id user-b))
          ingredients-a (controllers.ingredient/get-ingredients! db (:id user-a))
          ingredients-b (controllers.ingredient/get-ingredients! db (:id user-b))]
      (is (= 2 (count ingredients-a)))
      (is (= 1 (count ingredients-b))))))

(deftest find-ingredient-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          other-user (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))]
      (testing "returns ingredient for correct user"
        (is (some? (controllers.ingredient/find-ingredient! db (:id user) (str (:id ingredient))))))
      (testing "returns nil for wrong user"
        (is (nil? (controllers.ingredient/find-ingredient! db (:id other-user) (str (:id ingredient)))))))))

(deftest update-ingredient-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))]
      (testing "returns updated ingredient"
        (let [result (controllers.ingredient/update-ingredient! db (:id user) (str (:id ingredient)) {:name "Sugar"})]
          (is (= "Sugar" (:name result)))))
      (testing "returns nil for unknown id"
        (is (nil? (controllers.ingredient/update-ingredient! db (:id user) (str (java.util.UUID/randomUUID)) {:name "X"})))))))

(deftest create-ingredient-with-unit-size-test
  (with-rollback [db]
    (let [user   (factories/make-user! db)
          result (controllers.ingredient/create-ingredient! db (:id user) {:name "Soybean Oil" :base-unit "ml" :unit-size 900})]
      (is (= 900 (:unit-size result))))))

(deftest create-ingredient-unit-size-rejected-for-unit-base-test
  (with-rollback [db]
    (let [user (factories/make-user! db)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unit_size"
            (controllers.ingredient/create-ingredient! db (:id user) {:name "Eggs" :base-unit "unit" :unit-size 12}))))))

(deftest update-ingredient-unit-size-immutable-after-unit-purchases-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user) {:name "Soybean Oil" :base-unit "ml" :unit-size 900})]
      (factories/make-price! db (:id ingredient) {:package-quantity 1 :package-unit "unit"})
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unit_size"
            (controllers.ingredient/update-ingredient! db (:id user) (str (:id ingredient)) {:unit-size 500}))))))

(deftest update-ingredient-unit-size-allowed-before-unit-purchases-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user) {:name "Soybean Oil" :base-unit "ml" :unit-size 900})
          result     (controllers.ingredient/update-ingredient! db (:id user) (str (:id ingredient)) {:unit-size 500})]
      (is (= 500 (:unit-size result))))))

(deftest delete-ingredient-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))
          id         (str (:id ingredient))]
      (testing "first delete succeeds"
        (is (some? (controllers.ingredient/delete-ingredient! db (:id user) id))))
      (testing "second delete returns nil"
        (is (nil? (controllers.ingredient/delete-ingredient! db (:id user) id))))
      (testing "find returns nil after delete"
        (is (nil? (controllers.ingredient/find-ingredient! db (:id user) id)))))))
