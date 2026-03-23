(ns revenu-api.core
  (:require [revenu-api.components :as components]
            [revenu-api.db.migration :as migration])
  (:gen-class))

(defn -main [& _args]
  (migration/migrate)
  (components/bootstrap!))
