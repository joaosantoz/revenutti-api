(ns revenu-api.logic.recipe
  (:require [revenu-api.logic.unit-conversion :as logic.unit]))

(defn- ingredient-cost
  [ingredient]
  (let [{:keys [price-cents package-quantity package-unit usage-quantity usage-unit unit-size]} ingredient]
    (when (some? price-cents)
      (with-precision 10 :rounding HALF_UP
        (let [package-factor (if (and (= package-unit "unit") (some? unit-size))
                               (bigdec unit-size)
                               (bigdec (logic.unit/factor package-unit)))
              usage-factor   (bigdec (logic.unit/factor usage-unit))
              cost-per-base  (/ (bigdec price-cents)
                                (* (bigdec package-quantity) package-factor))
              cost           (* cost-per-base (* (bigdec usage-quantity) usage-factor))]
          cost)))))

(defn- calculate-totals
  [costs servings target-margin-bps]
  (with-precision 10 :rounding HALF_UP
    (let [total-cost                  (reduce + costs)
          portion-cost            (/ total-cost (bigdec servings))
          suggested-portion-price (* portion-cost
                                         (+ 1M (/ (bigdec target-margin-bps) 10000M)))
          suggested-total-price       (* suggested-portion-price (bigdec servings))]
      {:total-cost                  total-cost
       :portion-cost            portion-cost
       :suggested-portion-price suggested-portion-price
       :suggested-total-price       suggested-total-price})))

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
             :portion-cost            nil
             :suggested-portion-price nil
             :suggested-total-price       nil
             :ingredients                 ingredients}
      all-priced? (merge (calculate-totals costs servings target-margin-bps)))))
