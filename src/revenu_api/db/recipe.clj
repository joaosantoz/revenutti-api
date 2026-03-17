(ns revenu-api.db.recipe
  (:require [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [revenu-api.db.core :as db])
  (:import [java.util UUID]))

(defn- insert-ingredients! [tx recipe-id ingredients]
  (when (seq ingredients)
    (jdbc/execute! tx (sql/format
                        {:insert-into :recipe_ingredients
                         :values      (mapv (fn [{:keys [id usage_qty]}]
                                             {:recipe_id     recipe-id
                                              :ingredient_id (UUID/fromString id)
                                              :usage_qty     usage_qty})
                                           ingredients)}))))

(defn insert!
  [database user-id recipe]
  (db/with-transaction database tx
    (let [recipe-row (jdbc/execute-one! tx (sql/format
                                             {:insert-into :recipes
                                              :values      [{:user_id           user-id
                                                             :name              (:name recipe)
                                                             :yield_portions    (:yield_portions recipe)
                                                             :target_margin_bps (:target_margin_bps recipe)}]
                                              :returning   [:id :name :yield_portions :target_margin_bps :is_active :created_at :updated_at]}))]
      (insert-ingredients! tx (:recipes/id recipe-row) (:ingredients recipe))
      recipe-row)))

(defn- ingredients-agg
  []
  [:json_agg
   [:json_build_object
    "id"        :recipe_ingredients.ingredient_id
    "usage_qty" :recipe_ingredients.usage_qty
    "name"      :ingredients.name
    "unit"      :ingredients.unit]])

(defn get-all!
  [database user-id]
  (let [query {:select    [:recipes.* [(ingredients-agg) :ingredients]]
               :from      [:recipes]
               :left-join [:recipe_ingredients [:= :recipe_ingredients.recipe_id :recipes.id]
                           :ingredients        [:= :ingredients.id :recipe_ingredients.ingredient_id]]
               :where     [:and
                           [:= :recipes.is_active true]
                           [:= :recipes.user_id   user-id]]
               :group-by  [:recipes.id]}]
    (db/query database (sql/format query))))

(defn find!
  [database user-id id]
  (let [query {:select    [:recipes.* [(ingredients-agg) :ingredients]]
               :from      [:recipes]
               :left-join [:recipe_ingredients [:= :recipe_ingredients.recipe_id :recipes.id]
                           :ingredients        [:= :ingredients.id :recipe_ingredients.ingredient_id]]
               :where     [:and
                           [:= :recipes.id        (UUID/fromString id)]
                           [:= :recipes.is_active true]
                           [:= :recipes.user_id   user-id]]
               :group-by  [:recipes.id]}]
    (db/query-one database (sql/format query))))

(defn- delete-ingredients! [tx recipe-id]
  (jdbc/execute! tx (sql/format {:delete-from :recipe_ingredients
                                 :where       [:= :recipe_ingredients.recipe_id recipe-id]})))

(defn update!
  [database user-id id recipe]
  (db/with-transaction database tx
    (let [recipe-id (UUID/fromString id)
          row       (jdbc/execute-one! tx (sql/format
                                           {:update    :recipes
                                            :set       (cond-> {:updated_at [:now]}
                                                         (:name recipe)              (assoc :name (:name recipe))
                                                         (:yield_portions recipe)    (assoc :yield_portions (:yield_portions recipe))
                                                         (:target_margin_bps recipe) (assoc :target_margin_bps (:target_margin_bps recipe)))
                                            :where     [:and
                                                        [:= :recipes.id        recipe-id]
                                                        [:= :recipes.is_active true]
                                                        [:= :recipes.user_id   user-id]]
                                            :returning [:id]}))]
      (when row
        (when (contains? recipe :ingredients)
          (delete-ingredients! tx (:recipes/id row))
          (insert-ingredients! tx (:recipes/id row) (:ingredients recipe)))
        row))))

(defn deactivate!
  [database user-id id]
  (let [query {:update    :recipes
               :set       {:is_active  false
                           :updated_at [:now]}
               :where     [:and
                           [:= :recipes.id        (UUID/fromString id)]
                           [:= :recipes.is_active true]
                           [:= :recipes.user_id   user-id]]
               :returning [:id]}]
    (db/query-one database (sql/format query))))
