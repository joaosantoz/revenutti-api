(ns revenu-api.controllers.recipe
  (:require [clojure.string :as str]
            [revenu-api.adapters.recipe :as adapter]
            [revenu-api.db.ingredient :as ingredient.db]
            [revenu-api.db.recipe :as recipe.db]
            [revenu-api.logic.unit-conversion :as logic]))

(defn- validate-ingredients!
  [database user-id ingredients]
  (when (seq ingredients)
    (let [ids (map :id ingredients)
          found (ingredient.db/find-by-ids! database user-id ids)]
      (when (< (count found) (count ids))
        (throw (ex-info "One or more ingredients not found" {:status 422})))
      (let [base-unit-by-id (into {} (map (fn [row] [(str (:ingredients/id row))
                                                     (:ingredients/base_unit row)])
                                          found))]
        (doseq [{:keys [id usage_unit]} ingredients]
          (let [base-unit (get base-unit-by-id id)]
            (when-not (logic/compatible? base-unit usage_unit)
              (throw (ex-info (format "Invalid usage_unit '%s', accepted: %s"
                                      usage_unit (str/join ", " (logic/accepted-units base-unit)))
                              {:status 422})))))))))

(defn find-recipe!
  [database user-id id]
  (when-let [row (recipe.db/find! database user-id id)]
    (adapter/db->internal row)))

(defn create-recipe!
  [database user-id recipe]
  (validate-ingredients! database user-id (:ingredients recipe))
  (let [row (recipe.db/insert! database user-id recipe)]
    (find-recipe! database user-id (str (:recipes/id row)))))

(defn get-recipes!
  [database user-id]
  (->> (recipe.db/get-all! database user-id)
       (mapv adapter/db->internal)))

(defn update-recipe!
  [database user-id id recipe]
  (validate-ingredients! database user-id (:ingredients recipe))
  (when (recipe.db/update! database user-id id recipe)
    (find-recipe! database user-id id)))

(defn delete-recipe!
  [database user-id id]
  (recipe.db/deactivate! database user-id id))
