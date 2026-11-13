(ns mla.course-schema.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [mla.course-schema.dictionary :as d]))

(deftest part-of-speech-subtypes-validation-test
  (testing "Bare part of speech is valid"
    (is (true? (d/part-of-speech-subtypes-valid? :noun))))

  (testing "Valid subtype set"
    (is (true? (d/part-of-speech-subtypes-valid?
                [:noun #{:common :countable}]))))

  (testing "Empty subtype set is valid"
    (is (true? (d/part-of-speech-subtypes-valid?
                [:noun #{}]))))

  (testing "Valid multiple verb subtypes"
    (is (true? (d/part-of-speech-subtypes-valid?
                [:verb #{:transitive :intransitive}]))))

  (testing "Invalid subtype for a part of speech"
    (is (false? (d/part-of-speech-subtypes-valid?
                 [:noun #{:transitive}]))))

  (testing "Mixed valid and invalid subtypes"
    (is (false? (d/part-of-speech-subtypes-valid?
                 [:noun #{:common :transitive}]))))

  (testing "Subtype provided for a PoS that has no defined subtypes"
    (is (false? (d/part-of-speech-subtypes-valid?
                 [:preposition #{:demonstrative}])))))
