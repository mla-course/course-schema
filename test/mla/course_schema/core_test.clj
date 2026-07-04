(ns mla.course-schema.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.pprint :as pp]
            [malli.core :as m]
            [malli.error :as me]
            [mla.course-schema.core :as core]
            [mla.course-schema.dictionary :as d]
            [mla.course-schema.import :as imp]))

(defn- valid?
  [schema-key data]
  (let [explanation (m/explain schema-key data {:registry core/library-registry})]
    (if (nil? explanation)
      true
      (do
        (println "\n--- Malli Validation Failure ---")
        (pp/pprint (me/humanize explanation))
        false))))

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

(deftest import-spec-relaxation-test
  (testing "Strict LessonSpec rejects omitted IDs in nested exercises"
    (let [strict-lesson {:id (random-uuid)
                         :title "Test Lesson"
                         :exercises [{:type :sentence-builder
                                      :instruction "Translate to English"
                                      :words ["hello" "world"]
                                      :answer ["hello" "world"]}]}]
      (is (not (m/validate :schema/course-base
                           {:type :base
                            :id (random-uuid)
                            :title "Base Course"
                            :lessons [strict-lesson]}
                           {:registry core/library-registry}))))))

(deftest import-schema-entry-property-test
  (testing "a `{:generated true}` entry becomes optional"
    (let [Base   [:map
                  [:id {:generated true} :uuid]
                  [:title :string]]
          Import (imp/->import-schema Base)]
      (is (true? (m/validate Import {:title "no id needed"})))
      (is (false? (m/validate Base {:title "id is required here"})))))

  (testing "non-generated properties on an entry survive untouched"
    (let [Base   [:map
                  [:id {:generated true :description "server-assigned"} :uuid]
                  [:title :string]]
          Import (imp/->import-schema Base)
          id-entry (->> (m/children Import) (filter #(= :id (first %))) first)]
      (is (= {:generated true :description "server-assigned" :optional true}
             (second id-entry)))))

  (testing "fields that are already required (not :generated) stay required on import"
    (let [Base   [:map
                  [:id {:generated true} :uuid]
                  [:title :string]]
          Import (imp/->import-schema Base)]
      (is (false? (m/validate Import {:id (random-uuid)})))))

  (testing "relaxation recurses into nested maps and multi-schemas"
    (let [Item   [:map [:id {:generated true} :uuid] [:name :string]]
          Base   [:map [:items [:vector Item]]]
          Import (imp/->import-schema Base)]
      (is (true? (m/validate Import {:items [{:name "no id"}]})))
      (is (false? (m/validate Base {:items [{:name "no id"}]}))))))

(deftest course-level-generated-fields-relaxation-test
  (testing "course-base-import omits every compiler-generated field"
    (is (valid? :schema/course-base-import
                {:type :base
                 :title "Base Course"
                 :lessons []})))

  (testing "course-base (strict/export) still requires them all"
    (is (not (m/validate :schema/course-base
                         {:type :base
                          :title "Base Course"
                          :lessons []}
                         {:registry core/library-registry})))
    (let [errors (me/humanize (m/explain :schema/course-base
                                         {:type :base
                                          :title "Base Course"
                                          :lessons []}
                                         {:registry core/library-registry}))]
      (is (= #{:id :version :content-revision :created-by :created-at}
             (set (keys errors))))))

  (testing "non-generated required fields (e.g. :title) are still required on import"
    (is (not (m/validate :schema/course-base-import
                         {:type :base
                          :lessons []}
                         {:registry core/library-registry})))))
