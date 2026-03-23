(ns revenu-api.unit.adapters.user-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.adapters.user :as adapter])
  (:import [java.util UUID]))

(deftest db->internal-test
  (let [id (UUID/randomUUID)]
    (testing "maps users namespaced keys to internal"
      (is (= {:id                id
              :external-provider "google"
              :external-id       "1234567890"
              :name              "Joao Santos"
              :email             "joao@example.com"}
             (adapter/db->internal {:users/id                id
                                    :users/external_provider "google"
                                    :users/external_id       "1234567890"
                                    :users/name              "Joao Santos"
                                    :users/email             "joao@example.com"}))))))
