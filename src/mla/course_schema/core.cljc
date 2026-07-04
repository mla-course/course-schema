(ns mla.course-schema.core
  (:require [malli.core :as m]
            [mla.course-schema.assets :as a]
            [mla.course-schema.course :as c]
            [mla.course-schema.dictionary :as d]
            [mla.course-schema.exercise :as e]
            [mla.course-schema.primitives :as p]))

(def library-registry
  {:schema/locale                (m/schema p/Locale)
   :schema/asset                 (m/schema a/AssetSpec)
   :schema/language-entry        (m/schema d/LanguageEntrySpec)
   :schema/chinese-entry         (m/schema d/ChineseEntrySpec)
   :schema/exercise              (m/schema e/ExerciseSpec)
   :schema/exercise-import       (m/schema e/ExerciseImportSpec)
   :schema/course-base           (m/schema c/CourseSpec)
   :schema/course-base-import    (m/schema c/CourseImportSpec)
   :schema/course-language        (m/schema c/LanguageCourseSpec)
   :schema/course-language-import (m/schema c/LanguageCourseImportSpec)
   :schema/course-chinese         (m/schema c/ChineseCourseSpec)
   :schema/course-chinese-import  (m/schema c/ChineseCourseImportSpec)
   :schema/course-any             (m/schema c/AnyCourseSpec)
   :schema/course-any-import      (m/schema c/AnyCourseImportSpec)})

(defn- validate-with-registry [schema-key data]
  (m/validate schema-key data {:registry library-registry}))

(defn- explain-with-registry [schema-key data]
  (m/explain schema-key data {:registry library-registry}))

(defn valid-course? [data] (validate-with-registry :schema/course-any data))
(defn valid-strict-base-course? [data] (validate-with-registry :schema/course-base data))
(defn valid-language-course? [data] (validate-with-registry :schema/course-language data))
(defn valid-chinese-course? [data] (validate-with-registry :schema/course-chinese data))
(defn valid-language-entry? [data] (validate-with-registry :schema/language-entry data))
(defn valid-chinese-language-entry? [data]
  (validate-with-registry :schema/chinese-entry data))

(defn valid-import-course? [data] (validate-with-registry :schema/course-any-import data))
(defn valid-import-base-course? [data] (validate-with-registry :schema/course-base-import data))
(defn valid-import-language-course? [data] (validate-with-registry :schema/course-language-import data))
(defn valid-import-chinese-course? [data] (validate-with-registry :schema/course-chinese-import data))

(defn valid-import-exercise? [data]
  (validate-with-registry :schema/exercise-import data))

(defn explain-course [data] (explain-with-registry :schema/course-any data))
(defn explain-import-course [data] (explain-with-registry :schema/course-any-import data))
