(ns mla.course-schema.core
  (:require [malli.core :as m]
            [malli.util :as mu]
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
   :schema/course-base           (m/schema c/CourseSpec)
   :schema/course-language       (m/schema c/LanguageCourseSpec)
   :schema/course-chinese        (m/schema c/ChineseCourseSpec)
   :schema/course-any            (m/schema c/AnyCourseSpec)})

(defn- validate-with-registry [schema-key data]
  (m/validate schema-key data {:registry library-registry}))

(defn- explain-with-registry [schema-key data]
  (m/explain schema-key data {:registry library-registry}))

(defn valid-course? [data] (validate-with-registry :schema/course-any data))
(defn valid-base-course? [data] (validate-with-registry :schema/course-base data))
(defn valid-language-course? [data] (validate-with-registry :schema/course-language data))
(defn valid-chinese-course? [data] (validate-with-registry :schema/course-chinese data))
(defn valid-language-entry? [data] (validate-with-registry :schema/language-entry data))
(defn valid-chinese-language-entry? [data]
  (validate-with-registry :schema/chinese-entry data))

(defn valid-url? [data] (m/validate p/URL data))

(defn explain-course [data] (explain-with-registry :schema/course-any data))

(def ^:private course-specs
  {:base     c/CourseSpec
   :language c/LanguageCourseSpec
   :chinese  c/ChineseCourseSpec})

(defn course-keys
  "The keys a `course-type` course may carry."
  [course-type]
  (mu/keys (get course-specs course-type c/CourseSpec)))

(def lesson-keys
  "The keys a lesson may carry."
  (mu/keys c/LessonSpec))
