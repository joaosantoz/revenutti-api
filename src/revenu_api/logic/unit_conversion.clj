(ns revenu-api.logic.unit-conversion)

(def conversion-map
  {"kg"    {:base "g"    :factor 1000}
   "g"     {:base "g"    :factor 1}
   "l"     {:base "ml"   :factor 1000}
   "ml"    {:base "ml"   :factor 1}
   "dozen" {:base "unit" :factor 12}
   "unit"  {:base "unit" :factor 1}})

(def ^:private compatible-units
  {"g"    #{"g" "kg"}
   "ml"   #{"ml" "l"}
   "unit" #{"unit" "dozen"}})

(defn base-unit
  [unit]
  (get-in conversion-map [unit :base]))

(defn factor
  [unit]
  (get-in conversion-map [unit :factor]))

(defn compatible?
  [ingredient-base-unit unit]
  (contains? (get compatible-units ingredient-base-unit) unit))

(defn accepted-units
  [ingredient-base-unit]
  (get compatible-units ingredient-base-unit))
