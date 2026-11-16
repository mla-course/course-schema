(ns mla.course-schema.course
  (:require [malli.util :as mu]
            [mla.course-schema.assets :as a]
            [mla.course-schema.dictionary :as d]
            [mla.course-schema.exercise :as e]
            [mla.course-schema.hiccup :as h]
            [mla.course-schema.lang-block :as lb]
            [mla.course-schema.version :as v]
            [mla.course-schema.primitives :as p]))

(def CoursesCollectionSpec
  [:map
   [:created-at  p/DateTime]
   [:updated-at  {:optional true} p/DateTime]
   [:title       {:optional true} p/NonBlankStr]
   [:courses    [:vector :map]]])

(def ^:private CourseMetadataSpec
  [:map
   [:version           [:int {:min 0 :max v/CURRENT_VERSION}]]
   [:content-revision  [:int {:min 1}]]
   [:created-at        p/DateTime]
   [:updated-at        p/DateTime]
   [:published-at      {:optional true} p/DateTime]
   [:url               {:optional true} p/URL]
   [:assets-url        {:optional true} p/URL]
   [:repository        {:optional true} p/URL]
   [:source-url        {:optional true} p/URL]
   [:author            {:optional true} p/NonBlankStr]
   [:authors           {:optional true} [:vector p/NonBlankStr]]
   [:email             {:optional true} p/NonBlankStr]
   [:tags              {:optional true} [:vector [:string {:min 1}]]]
   [:difficulty        {:optional true}
    [:enum "beginner" "improver" "intermediate" "advanced" "expert"]]
   [:license           {:optional true} p/License]
   [:compatibility-ver {:optional true} p/SemVer]])

(def LessonSectionSpec
  [:map
   [:key :keyword]
   [:title h/TitleHiccupContent]
   [:content [:vector [:or h/Hiccup e/ExerciseSpec]]]])

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

(def AnyLessonSectionSpec
  [:multi {:dispatch :key :default :text}
   [:vocabulary VocabularySectionSpec]
   [:text       TextSectionSpec]
   [:exercises  ExercisesSectionSpec]])

(def ^:private LessonFieldsSpec
  [:map
   [:id          :uuid]
   [:title       {:optional true} h/TitleHiccupContent]
   [:description {:optional true} h/InlineHiccupContent]
   [:sections    {:optional true} [:vector AnyLessonSectionSpec]]
   [:content     {:optional true} [:vector [:or h/Hiccup e/ExerciseSpec]]]])

(def LessonSpec
  "Lesson fields plus per-lesson lang-block overrides."
  (mu/merge LessonFieldsSpec lb/LangBlockDefaultsSpec))

(def CreditsSpec
  [:map
   [:title p/NonBlankStr]
   [:content [:vector h/Hiccup]]])

(def ^:private CourseFieldsSpec
  "Fields shared by every course type"
  [:map
   [:id          :uuid]
   [:title       p/NonBlankStr]
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
           lb/LangBlockDefaultsSpec
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
