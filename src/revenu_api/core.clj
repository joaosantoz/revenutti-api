(ns revenu-api.core
  (:require [revenu-api.server :as server])
  (:gen-class))

(defn -main [& _args]
  (server/start))
