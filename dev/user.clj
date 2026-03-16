#_:clj-kondo/ignore
(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [revenu-api.server :as server]
            [revenu-api.migrate :as migrate]))

(defn start  [] (server/start))
(defn stop   [] (server/stop))
(defn reset  [] (stop) (refresh :after 'user/start))
(defn migrate  [] (migrate/migrate))
(defn rollback [] (migrate/rollback))
