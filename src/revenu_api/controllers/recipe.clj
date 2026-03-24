(ns revenu-api.controllers.recipe
  (:require [clojure.string :as str]
            [revenu-api.adapters.recipe :as adapter]
            [revenu-api.db.ingredient :as ingredient.db]
            [revenu-api.db.price-history :as price-history.db]
            [revenu-api.db.recipe :as recipe.db]
            [revenu-api.logic.recipe :as logic.recipe]
            [revenu-api.logic.unit-conversion :as logic]))

(defn- validate-ingredients!
  [database user-id ingredients]
  (when (seq ingredients)
    (let [ids (map :id ingredients)
          found (ingredient.db/find-by-ids! database user-id ids)]
      (when (< (count found) (count (set ids)))
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

(defn- enrich-with-prices
  [ingredients price-by-id unit-size-by-id]
  (mapv (fn [ingredient]
          (let [price (get price-by-id (:id ingredient))]
            {:id               (:id ingredient)
             :name             (:name ingredient)
             :brand            (:brand ingredient)
             :usage-quantity   (:usage_quantity ingredient)
             :usage-unit       (:usage_unit ingredient)
             :unit-size        (get unit-size-by-id (:id ingredient))
             :price-cents      (:price_history/price_cents price)
             :package-quantity (:price_history/package_quantity price)
             :package-unit     (:price_history/package_unit price)}))
        ingredients))

(defn- calculate-actual-margin
  [selling-price-total pricing-result]
  (when selling-price-total
    (logic.recipe/actual-margin
      (* (bigdec selling-price-total) 100M)
      (:total-cost pricing-result))))

(defn get-pricing!
  [database user-id id selling-price-total]
  (let [recipe (find-recipe! database user-id id)]
    (when-not recipe
      (throw (ex-info "Recipe not found" {:status 404})))
    (let [ingredients             (:ingredients recipe)
          ingredient-ids          (map :id ingredients)
          latest-prices           (price-history.db/latest-prices-for-ingredients! database ingredient-ids)
          price-by-id             (into {} (map (fn [price] [(str (:price_history/ingredient_id price)) price]) latest-prices))
          ingredient-details      (ingredient.db/find-by-ids! database user-id ingredient-ids)
          unit-size-by-id         (into {} (map (fn [row] [(str (:ingredients/id row)) (:ingredients/unit_size row)]) ingredient-details))
          ingredients-with-prices (enrich-with-prices ingredients price-by-id unit-size-by-id)
          pricing-result          (logic.recipe/recipe-pricing recipe ingredients-with-prices)
          actual-margin           (calculate-actual-margin selling-price-total pricing-result)]
      {:recipe          recipe
       :pricing-result  pricing-result
       :actual-margin   actual-margin})))
