(ns revenu-api.unit.interceptors-test
  (:require [clojure.test :refer [deftest is testing]]
            [revenu-api.interceptors :as interceptors]
            [revenu-api.infra.auth :as auth])
  (:import [java.util UUID]))

(defn- enter [interceptor ctx]
  ((:enter interceptor) ctx))

(defn- bearer-ctx [token]
  {:request {:headers {"authorization" (str "Bearer " token)}}})

(deftest jwt-interceptor-test
  (testing "missing Authorization header returns 401"
    (let [result (enter interceptors/jwt-interceptor {:request {:headers {}}})]
      (is (= 401 (get-in result [:response :status])))
      (is (= "Missing Authorization header" (get-in result [:response :body :detail])))))

  (testing "valid access token sets :user-id as UUID"
    (let [user-id (UUID/randomUUID)
          token   (auth/sign-access-token user-id)
          result  (enter interceptors/jwt-interceptor (bearer-ctx token))]
      (is (nil? (:response result)))
      (is (= user-id (get-in result [:request :user-id])))))

  (testing "refresh token used as Bearer returns 401"
    (let [user-id (UUID/randomUUID)
          token   (auth/sign-refresh-token user-id)
          result  (enter interceptors/jwt-interceptor (bearer-ctx token))]
      (is (= 401 (get-in result [:response :status])))
      (is (= "Invalid token type" (get-in result [:response :body :detail])))))

  (testing "malformed token returns 401"
    (let [result (enter interceptors/jwt-interceptor (bearer-ctx "not.a.token"))]
      (is (= 401 (get-in result [:response :status])))
      (is (= "Invalid or expired token" (get-in result [:response :body :detail])))))

  (testing "lowercase bearer prefix is accepted"
    (let [user-id (UUID/randomUUID)
          token   (auth/sign-access-token user-id)
          ctx     {:request {:headers {"authorization" (str "bearer " token)}}}
          result  (enter interceptors/jwt-interceptor ctx)]
      (is (nil? (:response result)))
      (is (= user-id (get-in result [:request :user-id]))))))
