(ns revenu-api.adapters.recipe
  (:require [cheshire.core :as json]
            [schema.core :as s]
            [revenu-api.wire.out.recipe :as out.recipe]
            [revenu-api.wire.in.recipe :as in.recipe]
            [revenu-api.models.recipe :as models.recipe]
            [revenu-api.wire.db.recipe :as db.recipe]))

(defn- cents->currency
  [amount]
  (when amount
    (.setScale (/ amount 100M) 2 java.math.RoundingMode/HALF_UP)))

(s/defn ^:private parse-ingredients :- (s/maybe [out.recipe/RecipeIngredient])
  [raw]
  (when raw
    (->> (json/parse-string (.getValue raw) true)
         (remove #(nil? (:id %)))
         (mapv #(if (nil? (:brand %)) (dissoc % :brand) %)))))

(s/defn wire->internal :- models.recipe/Recipe
  [recipe :- in.recipe/Recipe]
  (assoc (select-keys recipe [:name :servings :ingredients])
    :target-margin-bps (int (* (:target_margin recipe) 100M))))


(s/defn db->internal :- models.recipe/Recipe
  [row :- db.recipe/Recipe]
  (assoc {:id                (:recipes/id row)
          :name              (:recipes/name row)
          :servings          (:recipes/servings row)
          :target-margin-bps (:recipes/target_margin_bps row)
          :is-active         (:recipes/is_active row)}
    :ingredients (or (parse-ingredients (:ingredients row)) [])))

(s/defn update-wire->internal :- models.recipe/Recipe
  [recipe :- in.recipe/UpdateRecipe]
  (cond-> (select-keys recipe [:name :servings :ingredients])
    (:target_margin recipe) (assoc :target-margin-bps (int (* (:target_margin recipe) 100M)))))

(s/defn internal->wire :- out.recipe/Recipe
  [recipe :- models.recipe/Recipe]
  {:id            (:id recipe)
   :name          (:name recipe)
   :servings      (:servings recipe)
   :target_margin (double (/ (:target-margin-bps recipe) 100M))
   :ingredients   (:ingredients recipe)})

(defn- cost-ingredient->wire
  [ingredient]
  (cond-> {:id             (:id ingredient)
           :name           (:name ingredient)
           :usage_quantity (:usage-quantity ingredient)
           :usage_unit     (:usage-unit ingredient)
           :cost           (cents->currency (:cost ingredient))}
    (:brand ingredient) (assoc :brand (:brand ingredient))))

(s/defn pricing->wire :- out.recipe/RecipePricing
  [recipe         :- models.recipe/Recipe
   pricing-result :- models.recipe/RecipePricing
   actual-margin  :- (s/maybe s/Num)]
  (cond-> {:target_margin               (double (/ (:target-margin-bps recipe) 100M))
           :total_cost                  (cents->currency (:total-cost pricing-result))
           :cost_per_portion            (cents->currency (:cost-per-portion pricing-result))
           :suggested_price_per_portion (cents->currency (:suggested-price-per-portion pricing-result))
           :suggested_price_total       (cents->currency (:suggested-price-total pricing-result))
           :ingredients                 (mapv cost-ingredient->wire (:ingredients pricing-result))}
    actual-margin (assoc :actual_margin (double (.setScale actual-margin 2 java.math.RoundingMode/HALF_UP)))))
