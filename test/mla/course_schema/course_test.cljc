(ns mla.course-schema.course-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.pprint :as pp]
            [malli.core :as m]
            [malli.error :as me]
            [mla.course-schema.course :as sut]))

(defn- id [] #uuid "cc59006b-6df8-48c5-a1b1-2ebcf5058568")

(defn- invalid?
  "True when `data` does NOT satisfy `spec`."
  [spec data]
  (let [valid? (m/validate spec data)]
    (when valid?
      (some-> (m/explain spec data) me/humanize pp/pprint))
    (not valid?)))

(deftest course-schema-test
  (testing "valid base course"
    (is (m/validate sut/AnyCourseSpec
                    {:type :base
                     :id (id)
                     :title "Introduction to Logic"
                     :version 0
                     :content-revision 1
                     :created-by "admin"
                     :created-at "2026-07-04T12:00:00Z"
                     :lessons []})))

  (testing "valid language course"
    (is (m/validate sut/AnyCourseSpec
                    {:type :language
                     :id (id)
                     :title "Spanish 101"
                     :version 0
                     :content-revision 1
                     :created-by "admin"
                     :created-at "2026-07-04T12:00:00Z"
                     :source-lang :en
                     :target-lang :es
                     :lessons []})))

  (testing "valid chinese course"
    (is (m/validate sut/AnyCourseSpec
                    {:type :chinese
                     :id (id)
                     :title "Mandarin 101"
                     :version 0
                     :content-revision 1
                     :created-by "admin"
                     :created-at "2026-07-04T12:00:00Z"
                     :source-lang :en
                     :target-lang :zh-CN
                     :script :simplified
                     :romanisation :pinyin
                     :lessons []})))

  (testing "invalid course type or missing required fields"
    (is (invalid? sut/AnyCourseSpec
                  {:type :unknown
                   :id (id)
                   :title "Unknown"
                   :version 0
                   :content-revision 1
                   :created-by "admin"
                   :created-at "2026-07-04T12:00:00Z"}))
    (is (invalid? sut/AnyCourseSpec
                  {:type :language
                   :id (id)
                   :title "Missing source/target lang"
                   :version 0
                   :content-revision 1
                   :created-by "admin"
                   :created-at "2026-07-04T12:00:00Z"}))))
