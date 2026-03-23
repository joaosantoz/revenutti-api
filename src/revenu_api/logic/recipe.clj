(ns revenu-api.logic.recipe
  (:require [revenu-api.logic.unit-conversion :as logic.unit]))

(defn- ingredient-cost
  [ingredient]
  (let [{:keys [price-cents package-quantity package-unit usage-quantity usage-unit]} ingredient]
    (when (some? price-cents)
      (with-precision 10 :rounding HALF_UP
        (let [package-factor (bigdec (logic.unit/factor package-unit))
              usage-factor   (bigdec (logic.unit/factor usage-unit))
              cost-per-base  (/ (bigdec price-cents)
                                (* (bigdec package-quantity) package-factor))
              cost           (* cost-per-base (* (bigdec usage-quantity) usage-factor))]
          cost)))))

(defn- calculate-totals
  [costs servings target-margin-bps]
  (with-precision 10 :rounding HALF_UP
    (let [total-cost                  (reduce + costs)
          cost-per-portion            (/ total-cost (bigdec servings))
          suggested-price-per-portion (* cost-per-portion
                                         (+ 1M (/ (bigdec target-margin-bps) 10000M)))
          suggested-price-total       (* suggested-price-per-portion (bigdec servings))]
      {:total-cost                  total-cost
       :cost-per-portion            cost-per-portion
       :suggested-price-per-portion suggested-price-per-portion
       :suggested-price-total       suggested-price-total})))

(defn actual-margin
  [selling-price-total-cents total-cost-cents]
  (when (and (some? total-cost-cents) (pos? total-cost-cents))
    (with-precision 10 :rounding HALF_UP
      (* (/ (- (bigdec selling-price-total-cents) (bigdec total-cost-cents))
            (bigdec total-cost-cents))
         100M))))

(defn recipe-pricing
  [recipe ingredients-with-prices]
  (let [{:keys [servings target-margin-bps]} recipe
        costs       (mapv ingredient-cost ingredients-with-prices)
        ingredients (mapv (fn [ingredient cost] (assoc ingredient :cost cost)) ingredients-with-prices costs)
        all-priced? (every? some? costs)]
    (cond-> {:total-cost                  nil
             :cost-per-portion            nil
             :suggested-price-per-portion nil
             :suggested-price-total       nil
             :ingredients                 ingredients}
      all-priced? (merge (calculate-totals costs servings target-margin-bps)))))
