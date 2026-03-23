(ns revenu-api.helpers.factories
  (:require [revenu-api.adapters.ingredient :as adapter.ingredient]
            [revenu-api.adapters.price-history :as adapter.price-history]
            [revenu-api.adapters.user :as adapter.user]
            [revenu-api.controllers.recipe :as controllers.recipe]
            [revenu-api.db.ingredient :as db.ingredient]
            [revenu-api.db.price-history :as db.price-history]
            [revenu-api.db.user :as db.user]
            [revenu-api.db.recipe :as db.recipe]
            [revenu-api.infra.auth :as auth])
  (:import [java.util UUID]))

(defn make-user!
  [database & [overrides]]
  (-> (db.user/upsert-user! database (merge {:external-provider "google"
                                             :external-id       (str (UUID/randomUUID))
                                             :name              "Joao Santos"
                                             :email             "joao@example.com"}
                                            overrides))
      (adapter.user/db->internal)))

(defn make-ingredient!
  [database user-id & [overrides]]
  (let [row (db.ingredient/insert! database user-id (merge {:name      "Flour"
                                                            :base-unit "g"}
                                                           overrides))]
    (-> (db.ingredient/find! database user-id (str (:ingredients/id row)))
        (adapter.ingredient/db->internal))))

(defn make-price!
  [database ingredient-id & [overrides]]
  (-> (db.price-history/insert! database
                                 (str ingredient-id)
                                 (merge {:price-cents      500
                                         :package-quantity 1000
                                         :package-unit     "g"
                                         :purchase-date    "2024-01-01"}
                                        overrides))
      (adapter.price-history/db->internal)))

(defn make-recipe!
  [database user-id & [overrides]]
  (let [row (db.recipe/insert! database user-id (merge {:name              "Carrot Cake"
                                                        :servings          8
                                                        :target-margin-bps 5000
                                                        :ingredients       []}
                                                       overrides))]
    (controllers.recipe/find-recipe! database user-id (str (:recipes/id row)))))

(defn make-access-token
  [user-id]
  (auth/sign-access-token user-id))

(defn make-refresh-token
  [user-id]
  (auth/sign-refresh-token user-id))
