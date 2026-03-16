(ns revenu-api.db.ingredient
  (:require [honey.sql :as sql]
            [revenu-api.db.core :as db])
  (:import (java.util UUID)))

(defn insert! [database ingredient]
  (let [query {:insert-into :ingredients
               :values      [{:name    (:name ingredient)
                              :unit    (name (:unit ingredient))
                              :user_id #uuid "1d5c42d5-0432-47c3-abc2-82045e7f40b8"}]
               :returning   [:id :name :unit :is_active :created_at]}]
    (db/query-one database (sql/format query))))
