(ns revenu-api.integration.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.controllers.auth :as controllers.auth]
            [revenu-api.db.refresh-token :as db.refresh-token]
            [revenu-api.db.user :as db.user]
            [revenu-api.helpers.db :refer [with-rollback]]
            [revenu-api.helpers.factories :as factories]
            [revenu-api.infra.auth :as auth])
  (:import [java.util UUID]))

(defn- store-refresh-token!
  [database user-id]
  (let [token (auth/sign-refresh-token user-id)]
    (db.refresh-token/insert! database user-id
                               (auth/hash-token token)
                               (auth/refresh-token-expires-at))
    token))

(deftest refresh-tokens-valid-test
  (with-rollback [db]
    (let [user          (factories/make-user! db)
          refresh-token (store-refresh-token! db (:id user))
          result        (controllers.auth/refresh-tokens! db refresh-token)]
      (is (string? (:access-token result)))
      (is (string? (:refresh-token result)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"revoked"
            (controllers.auth/refresh-tokens! db refresh-token))))))

(deftest refresh-tokens-wrong-type-test
  (with-rollback [db]
    (let [user         (factories/make-user! db)
          access-token (factories/make-access-token (:id user))]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid token type"
            (controllers.auth/refresh-tokens! db access-token))))))

(deftest refresh-tokens-already-revoked-test
  (with-rollback [db]
    (let [user          (factories/make-user! db)
          refresh-token (store-refresh-token! db (:id user))
          logout-result (controllers.auth/logout! db refresh-token)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"revoked"
            (controllers.auth/refresh-tokens! db refresh-token))))))

(deftest upsert-user-same-external-id-test
  (with-rollback [db]
    (let [external-id   (str (UUID/randomUUID))
          user-data     {:external-provider "google"
                         :external-id       external-id
                         :name              "Joao Santos"
                         :email             "joao@example.com"}
          first-result  (db.user/upsert-user! db user-data)
          second-result (db.user/upsert-user! db (assoc user-data :name "Joao Updated"))]
      (testing "same UUID returned on second upsert"
        (is (= (:users/id first-result) (:users/id second-result))))
      (testing "name is updated on second upsert"
        (is (= "Joao Updated" (:users/name second-result)))))))
