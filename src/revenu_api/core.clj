(ns revenu-api.core
  (:require [revenu-api.components :as components])
  (:gen-class))

(defn -main [& _args]
  (components/bootstrap!))
