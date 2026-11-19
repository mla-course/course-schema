(ns mla.course-schema.core
  (:require [malli.core :as m]
            [malli.util :as mu]
            [mla.course-schema.assets :as a]
            [mla.course-schema.course :as c]
            [mla.course-schema.dictionary :as d]
            [mla.course-schema.exercise :as e]
            [mla.course-schema.primitives :as p]))

(def library-registry
  {:schema/locale          (m/schema p/Locale)
   :schema/asset           (m/schema a/AssetSpec)
   :schema/language-entry  (m/schema d/LanguageEntrySpec)
   :schema/chinese-entry   (m/schema d/ChineseEntrySpec)
   :schema/exercise        (m/schema e/ExerciseSpec)
   :schema/course-base     (m/schema c/CourseSpec)
   :schema/course-language (m/schema c/LanguageCourseSpec)
   :schema/course-chinese  (m/schema c/ChineseCourseSpec)
   :schema/course-any      (m/schema c/AnyCourseSpec)})

(def ^:private compiled
  "Registry key -> schema resolved against `library-registry`, built once."
  (update-vals library-registry #(m/schema % {:registry library-registry})))

(defn- validator-of [k] (m/validator (compiled k)))
(defn- explainer-of [k] (m/explainer (compiled k)))

;; Validators / explainers are built at namespace load, not per call.

(def valid-course?                 (validator-of :schema/course-any))
(def valid-base-course?            (validator-of :schema/course-base))
(def valid-language-course?        (validator-of :schema/course-language))
(def valid-chinese-course?         (validator-of :schema/course-chinese))
(def valid-language-entry?         (validator-of :schema/language-entry))
(def valid-chinese-language-entry? (validator-of :schema/chinese-entry))
(def valid-url?                    (m/validator p/URL))

(def explain-course   (explainer-of :schema/course-any))
(def explain-exercise (explainer-of :schema/exercise))

(def ^:private course-key-sets
  {:base     (mu/keys c/CourseSpec)
   :language (mu/keys c/LanguageCourseSpec)
   :chinese  (mu/keys c/ChineseCourseSpec)})

(defn course-keys
  "The keys a `course-type` course may carry."
  [course-type]
  (get course-key-sets course-type (:base course-key-sets)))

(def lesson-keys
  "The keys a lesson may carry."
  (mu/keys c/LessonSpec))

(defn exercise?
  "Whether `x` is a map of one of the exercise kinds."
  [x]
  (and (map? x) (contains? e/exercise-types (:type x))))
