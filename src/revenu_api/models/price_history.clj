(ns revenu-api.models.price-history
  (:require [schema.core :as s]))

(s/defschema PriceHistory
  {:id            s/Uuid
   :ingredient-id s/Uuid
   :price-cents   s/Int
   :package-size   s/Int
   :purchase-date s/Str})
