(ns mla.course-schema.migration-test
  (:require [clojure.test :refer [deftest testing is]]
            [mla.course-schema.migration :as migration]
            [mla.course-schema.version :as v]))

(deftest detect-version-test
  (testing "Missing :version defaults to 0 (pre-versioning data)"
    (is (= 0 (migration/detect-version {:id "x"}))))
  (testing "Non-integer :version is treated as 0, defensively"
    (is (= 0 (migration/detect-version {:id "x" :version "1"}))))
  (testing "A well-formed integer version is read through as-is"
    (is (= 3 (migration/detect-version {:id "x" :version 3})))))

(deftest migrate-current-version-test
  (testing "A course already at CURRENT_VERSION passes through untouched and reports migrated? false"
    (let [course {:id "x" :version v/CURRENT_VERSION}
          result (migration/migrate course)]
      (is (= course (:course result)))
      (is (false? (:migrated? result)))
      (is (= v/CURRENT_VERSION (:to-version result))))))

(deftest migrate-applies-registered-steps-test
  (testing "Each registered step runs in order, chaining from-version -> to-version"
    (with-redefs [v/CURRENT_VERSION 2
                  migration/migrations {0 #(update % :version inc)
                                        1 #(-> % (update :version inc) (assoc :migrated-at-v1 true))}]
      (let [result (migration/migrate {:id "x" :version 0})]
        (is (:migrated? result))
        (is (= 0 (:from-version result)))
        (is (= 2 (:to-version result)))
        (is (= {:id "x" :version 2 :migrated-at-v1 true} (:course result)))))))

(deftest migrate-missing-step-test
  (testing "Migrating from a version with no registered step throws a descriptive ex-info,
            rather than silently returning unmigrated data"
    (with-redefs [v/CURRENT_VERSION 1
                  migration/migrations {}]
      (let [course {:id "x" :version 0}]
        (try
          (migration/migrate course)
          (is false) ; should be unreachable
          (catch clojure.lang.ExceptionInfo e
            (is (re-find #"No migration registered" (ex-message e)))))))))

(deftest rollback-same-version-test
  (testing "Rolling back to the version already held passes through untouched"
    (let [course {:id "x" :version 2}
          result (migration/rollback course 2)]
      (is (= course (:course result)))
      (is (false? (:migrated? result))))))

(deftest rollback-applies-registered-steps-test
  (testing "Each registered rollback runs in descending order, chaining from-version -> target"
    (with-redefs [migration/rollbacks {2 #(-> % (update :version dec) (dissoc :migrated-at-v1))
                                        1 #(update % :version dec)}]
      (let [result (migration/rollback {:id "x" :version 2 :migrated-at-v1 true} 0)]
        (is (:migrated? result))
        (is (= 2 (:from-version result)))
        (is (= 0 (:to-version result)))
        (is (= {:id "x" :version 0} (:course result)))))))

(deftest rollback-missing-step-test
  (testing "Rolling back through a version with no registered step throws a descriptive ex-info"
    (with-redefs [migration/rollbacks {}]
      (try
        (migration/rollback {:id "x" :version 1} 0)
        (is false) ; should be unreachable
        (catch clojure.lang.ExceptionInfo e
          (is (re-find #"No migration registered" (ex-message e))))))))
