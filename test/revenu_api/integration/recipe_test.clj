(ns revenu-api.integration.recipe-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.controllers.recipe :as controllers.recipe]
            [revenu-api.helpers.db :refer [with-rollback]]
            [revenu-api.helpers.factories :as factories]))

(deftest create-recipe-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))
          recipe     {:name              "Carrot Cake"
                      :servings          8
                      :target-margin-bps 5000
                      :ingredients       [{:id             (str (:id ingredient))
                                           :usage_quantity 200
                                           :usage_unit     "g"}]}
          result     (controllers.recipe/create-recipe! db (:id user) recipe)]
      (is (uuid? (:id result)))
      (is (= "Carrot Cake" (:name result)))
      (is (= 1 (count (:ingredients result))))
      (is (= (str (:id ingredient)) (:id (first (:ingredients result))))))))

(deftest create-recipe-unknown-ingredient-test
  (with-rollback [db]
    (let [user   (factories/make-user! db)
          recipe {:name              "Cake"
                  :servings          4
                  :target-margin-bps 5000
                  :ingredients       [{:id             (str (java.util.UUID/randomUUID))
                                       :usage_quantity 100
                                       :usage_unit     "g"}]}]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not found"
            (controllers.recipe/create-recipe! db (:id user) recipe))))))

(deftest create-recipe-other-users-ingredient-test
  (with-rollback [db]
    (let [user-a     (factories/make-user! db)
          user-b     (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user-b))
          recipe     {:name              "Cake"
                      :servings          4
                      :target-margin-bps 5000
                      :ingredients       [{:id             (str (:id ingredient))
                                           :usage_quantity 100
                                           :usage_unit     "g"}]}]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not found"
            (controllers.recipe/create-recipe! db (:id user-a) recipe))))))

(deftest update-recipe-replaces-ingredients-test
  (with-rollback [db]
    (let [user        (factories/make-user! db)
          ingredient  (factories/make-ingredient! db (:id user))
          recipe      (factories/make-recipe! db (:id user))
          update-data {:ingredients [{:id             (str (:id ingredient))
                                      :usage_quantity 500
                                      :usage_unit     "g"}]}
          result      (controllers.recipe/update-recipe! db (:id user) (str (:id recipe)) update-data)]
      (is (= 1 (count (:ingredients result))))
      (is (= (str (:id ingredient)) (:id (first (:ingredients result))))))))

(deftest update-recipe-without-ingredients-key-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))
          recipe     (factories/make-recipe! db (:id user) {:ingredients [{:id             (str (:id ingredient))
                                                                           :usage_quantity 100
                                                                           :usage_unit     "g"}]})
          result     (controllers.recipe/update-recipe! db (:id user) (str (:id recipe)) {:name "New Name"})]
      (is (= "New Name" (:name result)))
      (is (= 1 (count (:ingredients result)))))))

(deftest get-recipes-tenant-isolation-test
  (with-rollback [db]
    (let [user-a    (factories/make-user! db)
          user-b    (factories/make-user! db)
          recipe-a1 (factories/make-recipe! db (:id user-a))
          recipe-a2 (factories/make-recipe! db (:id user-a))
          recipe-b  (factories/make-recipe! db (:id user-b))
          recipes-a (controllers.recipe/get-recipes! db (:id user-a))
          recipes-b (controllers.recipe/get-recipes! db (:id user-b))]
      (is (= 2 (count recipes-a)))
      (is (= 1 (count recipes-b))))))

(deftest get-pricing-with-priced-ingredient-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ;; 1000g package for 500 cents, use 500g → cost = 250 cents
          ingredient (factories/make-ingredient! db (:id user))
          price      (factories/make-price! db (:id ingredient) {:price-cents      500
                                                                  :package-quantity 1000
                                                                  :package-unit     "g"
                                                                  :purchase-date    "2024-01-01"})
          recipe     (factories/make-recipe! db (:id user) {:servings          2
                                                            :target-margin-bps 5000
                                                            :ingredients       [{:id             (str (:id ingredient))
                                                                                 :usage_quantity 500
                                                                                 :usage_unit     "g"}]})
          {:keys [pricing-result]} (controllers.recipe/get-pricing! db (:id user) (str (:id recipe)) nil)]
      (testing "total cost is calculated correctly"
        (is (= 250M (:total-cost pricing-result))))
      (testing "cost per portion is total divided by servings"
        (is (= 125M (:portion-cost pricing-result))))
      (testing "suggested price applies markup"
        ;; 125 * (1 + 50%) = 187.5 per portion, 375 total
        (is (= 187.5M (:suggested-portion-price pricing-result)))
        (is (= 375M   (:suggested-total-price pricing-result))))
      (testing "ingredient cost is attached"
        (is (= 250M (:cost (first (:ingredients pricing-result)))))))))

(deftest get-pricing-with-unpriced-ingredient-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))
          recipe     (factories/make-recipe! db (:id user) {:ingredients [{:id             (str (:id ingredient))
                                                                           :usage_quantity 100
                                                                           :usage_unit     "g"}]})
          {:keys [pricing-result]} (controllers.recipe/get-pricing! db (:id user) (str (:id recipe)) nil)]
      (testing "all cost fields are nil when ingredient has no price"
        (is (nil? (:total-cost pricing-result)))
        (is (nil? (:portion-cost pricing-result)))
        (is (nil? (:suggested-portion-price pricing-result)))
        (is (nil? (:suggested-total-price pricing-result))))
      (testing "ingredient cost is nil"
        (is (nil? (:cost (first (:ingredients pricing-result)))))))))

(deftest get-pricing-with-actual-margin-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user))
          price      (factories/make-price! db (:id ingredient) {:price-cents      500
                                                                  :package-quantity 1000
                                                                  :package-unit     "g"
                                                                  :purchase-date    "2024-01-01"})
          recipe     (factories/make-recipe! db (:id user) {:servings          2
                                                            :target-margin-bps 5000
                                                            :ingredients       [{:id             (str (:id ingredient))
                                                                                 :usage_quantity 500
                                                                                 :usage_unit     "g"}]})
          ;; total cost = 250 cents, selling for 500 cents → 100% markup
          {:keys [actual-margin]} (controllers.recipe/get-pricing! db (:id user) (str (:id recipe)) 5.0)]
      (is (some? actual-margin))
      (is (= 100M actual-margin)))))

(deftest get-pricing-with-unit-size-test
  (with-rollback [db]
    (let [user       (factories/make-user! db)
          ingredient (factories/make-ingredient! db (:id user) {:name "Soybean Oil" :base-unit "ml" :unit-size 900})
          ;; 1 bottle of 900ml at 900 cents → 1 cent/ml
          _          (factories/make-price! db (:id ingredient) {:price-cents      900
                                                                  :package-quantity 1
                                                                  :package-unit     "unit"
                                                                  :purchase-date    "2024-01-01"})
          recipe     (factories/make-recipe! db (:id user) {:servings          1
                                                            :target-margin-bps 0
                                                            :ingredients       [{:id             (str (:id ingredient))
                                                                                 :usage_quantity 100
                                                                                 :usage_unit     "ml"}]})
          {:keys [pricing-result]} (controllers.recipe/get-pricing! db (:id user) (str (:id recipe)) nil)]
      ;; cost-per-ml = 900 / (1 * 900) = 1 cent/ml
      ;; ingredient-cost = 1 * 100 = 100 cents
      (is (= 100M (:total-cost pricing-result))))))

(deftest get-pricing-recipe-not-found-test
  (with-rollback [db]
    (let [user (factories/make-user! db)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"not found"
            (controllers.recipe/get-pricing! db (:id user) (str (java.util.UUID/randomUUID)) nil))))))

(deftest delete-recipe-test
  (with-rollback [db]
    (let [user   (factories/make-user! db)
          recipe (factories/make-recipe! db (:id user))
          id     (str (:id recipe))]
      (testing "first delete succeeds"
        (is (some? (controllers.recipe/delete-recipe! db (:id user) id))))
      (testing "second delete returns nil"
        (is (nil? (controllers.recipe/delete-recipe! db (:id user) id))))
      (testing "find returns nil after delete"
        (is (nil? (controllers.recipe/find-recipe! db (:id user) id)))))))
