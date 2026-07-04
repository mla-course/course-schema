(ns mla.course-schema.course
  (:require [malli.util :as mu]
            [mla.course-schema.assets :as a]
            [mla.course-schema.dictionary :as d]
            [mla.course-schema.exercise :as e]
            [mla.course-schema.hiccup :as h]
            [mla.course-schema.import :as imp]
            [mla.course-schema.version :as v]
            [mla.course-schema.primitives :as p]))

(def CoursesCollectionSpec
  [:map
   [:created-at  {:generated true} p/DateTime]
   [:updated-at  {:optional true} p/DateTime]
   [:title       {:optional true} p/NonBlankStr]
   [:courses    [:vector :map]]])

(def ^:private CourseMetadataSpec
  [:map
   [:version           {:generated true} [:int {:min 0 :max v/CURRENT_VERSION}]]
   [:content-revision  {:generated true} [:int {:min 1}]]
   [:created-by        {:generated true} p/NonBlankStr]
   [:created-at        {:generated true} p/DateTime]
   [:updated-at        {:optional true} p/DateTime]
   [:updated-by        {:optional true} p/NonBlankStr]
   [:published-at      {:optional true} p/DateTime]
   [:url               {:optional true} p/URL]
   ;; any relative path to media asset will be resolved against this url
   ;; forming fulpath, when not present the fallback is to use `:url`
   [:assets-url        {:optional true} p/URL]
   [:repository        {:optional true} p/URL]
   [:org-mode-source-url {:optional true} p/URL]
   [:author            {:optional true} p/NonBlankStr]
   [:authors           {:optional true} [:vector p/NonBlankStr]]
   [:email             {:optional true} p/NonBlankStr]
   [:tags              {:optional true} [:vector [:string {:min 1}]]]
   [:difficulty        {:optional true}
    [:enum "beginner" "improver" "intermediate" "advanced" "expert"]]
   [:license           {:optional true} p/License]
   [:compatibility-ver {:optional true} p/SemVer]])

;; --- Base / Fallback Section Spec ---
(def LessonSectionSpec
  [:map
   [:key :keyword]
   [:title {:optional true} h/InlineHiccupContent]
   [:icon {:optional true} p/NonBlankStr]
   [:content {:optional true} [:vector [:or h/Hiccup e/ExerciseSpec]]]])

(def TextSectionSpec
  (mu/merge
   LessonSectionSpec
   [:map
    [:key [:= :text]]]))

;; --- Vocabulary Sections

(def ^:private BaseVocabularySectionSpec
  (mu/merge
   LessonSectionSpec
   [:map
    [:key [:= :vocabulary]]]))

(def StandardVocabularySectionSpec
  (mu/merge
   BaseVocabularySectionSpec
   [:map
    [:type [:= :standard]]
    [:content [:vector d/LanguageEntrySpec]]]))

(def ChineseVocabularySectionSpec
  (mu/merge
   BaseVocabularySectionSpec
   [:map
    [:type [:= :chinese]]
    [:content [:vector d/ChineseEntrySpec]]]))

(def VocabularySectionSpec
  [:multi {:dispatch :type}
   [:standard StandardVocabularySectionSpec]
   [:chinese  ChineseVocabularySectionSpec]])

(def ExercisesSectionSpec
  (mu/merge
   LessonSectionSpec
   [:map
    [:key [:= :exercises]]
    [:content [:vector e/ExerciseSpec]]]))

;; --- Top-Level Polymorphic Section Dispatcher ---

(def AnyLessonSectionSpec
  [:multi {:dispatch :key :default :text}
   [:vocabulary VocabularySectionSpec]
   [:text       TextSectionSpec]
   [:exercises  ExercisesSectionSpec]])

;; =============================================================================
;; Section Import Specs
;; =============================================================================

(def AnyLessonSectionImportSpec
  [:multi {:dispatch :key :default :text}
   [:vocabulary VocabularySectionSpec]
   [:text       TextSectionSpec]
   [:exercises  ExercisesSectionSpec]])

;; =============================================================================
;; Lessons
;; =============================================================================

(def LessonSpec
  [:map
   [:id          {:generated true} :uuid]
   [:title       h/InlineHiccupContent]
   [:description {:optional true} h/InlineHiccupContent]
   [:icon        {:optional true} p/NonBlankStr]
   [:sections    {:optional true} [:vector AnyLessonSectionSpec]]
   [:content     {:optional true} [:vector [:or h/Hiccup e/ExerciseImportSpec]]]])

;; =============================================================================
;; Lesson Imports
;; =============================================================================

(def LessonImportSpec
  [:and
   (imp/->import-schema LessonSpec)
   [:map
    [:sections {:optional true} [:vector AnyLessonSectionImportSpec]]]])

;; =============================================================================
;; Courses
;; =============================================================================

(def CreditsSpec
  [:map
   [:title p/NonBlankStr]
   [:content [:vector h/Hiccup]]])

(def ^:private CourseFieldsSpec
  "Fields shared by every course type"
  [:map
   [:id          {:generated true} :uuid]
   [:title       p/NonBlankStr]
   [:icon        {:optional true} p/NonBlankStr]
   [:description {:optional true} p/NonBlankStr]
   [:summary     {:optional true} p/NonBlankStr]
   [:lang        {:optional true} p/Locale]
   [:credits     {:optional true} CreditsSpec]
   [:lessons     {:optional true} [:vector LessonSpec]]
   [:review      {:optional true} [:map [:exercises
                                         {:optional true}
                                         [:vector e/ExerciseSpec]]]]
   [:assets      {:optional true} [:vector a/AssetSpec]]])

(def CourseSpec
  "A generic course with no domain assumptions."
  (reduce mu/merge
          [CourseMetadataSpec
           CourseFieldsSpec
           [:map
            {:title "Base Course"
             :description "A generic course shell containing ordered lessons."
             :example {:type :base
                       :id #uuid "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd"
                       :title "Introduction to Logic"
                       :version 0
                       :content-revision 1
                       :created-by "admin"
                       :created-at "2026-07-04T12:00:00Z"
                       :lessons []}}
            [:type [:= :base]]]]))

(def LanguageCourseSpec
  (mu/merge
   CourseSpec
   [:map
    [:type        [:= :language]]
    [:source-lang p/Locale]
    [:target-lang p/Locale]]))

(def ChineseCourseSpec
  (mu/merge
   LanguageCourseSpec
   [:map
    [:type         [:= :chinese]]
    [:script       {:optional true} [:enum :simplified :traditional :both]]
    [:romanisation {:optional true} [:enum :pinyin :wade-giles :jyutping]]]))

(def AnyCourseSpec
  [:multi {:dispatch :type}
   [:base     CourseSpec]
   [:language LanguageCourseSpec]
   [:chinese  ChineseCourseSpec]])

;; =============================================================================
;; Course Imports
;; =============================================================================

(defn- ->course-import-spec
  "The lenient authoring counterpart of the export course `spec`:
   generated fields become optional, and lessons are checked against
   `LessonImportSpec`. `:lessons` stays optional, as it is on export."
  [spec]
  [:and (imp/->import-schema spec)
   [:map [:lessons [:vector LessonImportSpec]]]])

(def CourseImportSpec         (->course-import-spec CourseSpec))
(def LanguageCourseImportSpec (->course-import-spec LanguageCourseSpec))
(def ChineseCourseImportSpec  (->course-import-spec ChineseCourseSpec))

(def AnyCourseImportSpec
  [:multi {:dispatch :type}
   [:base     CourseImportSpec]
   [:language LanguageCourseImportSpec]
   [:chinese  ChineseCourseImportSpec]])
