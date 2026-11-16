(ns mla.course-schema.lang-block
  "Document-wide defaults for multi-language blocks and ruby macros."
  (:require [mla.course-schema.primitives :as p]))

(def LanguageGroupSpec
  "One display group: a `:label` and its language layers, in order."
  [:map
   [:label :keyword]
   [:langs [:vector {:min 1} p/Locale]]])

(def LangBlockDefaultsSpec
  "Defaults for a block's own `:languages`/`:translation`/`:annotate` args."
  [:map
   [:lang-block-languages   {:optional true} [:vector {:min 1} LanguageGroupSpec]]
   [:lang-block-translation {:optional true} p/Locale]
   [:lang-block-annotate    {:optional true} [:map-of p/Locale :keyword]]
   ;; TODO: this is only needed for org -> edn direction
   ;; [:ruby-languages         {:optional true} [:vector {:min 1} p/Locale]]
   ])
