(ns mla.course-schema.assets
  (:require [mla.course-schema.primitives :as p]))

(def VideoAccessibilitySpec
  [:map
   [:alt-text p/NonBlankStr]
   [:alt-text-lang {:optional true} p/Locale]])

(def ImageAccessibilitySpec
  [:map
   [:alt-text p/NonBlankStr]
   [:alt-text-lang {:optional true} p/Locale]
   [:long-description {:optional true} p/NonBlankStr]
   [:high-contrast-url {:optional true} p/URL]
   [:is-colorblind-safe {:optional true} :boolean]])

(def LicensingSpec
  [:map
   [:license p/License]
   [:credit {:optional true} p/Attribution]])

(def BaseAssetSpec
  [:map
   [:id {:optional true} :uuid]
   [:url p/URL]
   [:title {:optional true} p/NonBlankStr]
   [:licensing LicensingSpec]
   [:source {:optional true} p/Attribution]])

;; --- Quality variants ---------------------------------------------------

(def ImageVariantSpec
  [:map
   [:url     p/URL]
   [:width   {:optional true} [:int {:min 1}]]
   [:height  {:optional true} [:int {:min 1}]]
   [:quality {:optional true} p/NonBlankStr]])

(def VideoVariantSpec
  [:map
   [:url     p/URL]
   [:quality {:optional true} p/NonBlankStr] ; e.g. "480p" "720p" "1080p"
   [:width   {:optional true} [:int {:min 1}]]
   [:height  {:optional true} [:int {:min 1}]]
   [:bitrate {:optional true} [:int {:min 1}]] ; bits per second
   [:codec   {:optional true} p/NonBlankStr]])

(def ExternalSourceSpec
  "Where a hosted asset lives on a third-party service."
  [:map
   [:service p/NonBlankStr]
   [:id      p/NonBlankStr]])

(def AssetSpec
  [:multi {:dispatch :asset-type}
   [:audio
    [:and
     BaseAssetSpec
     [:map
      [:asset-type [:= :audio]]]]]
   [:video
    [:and
     BaseAssetSpec
     [:map
      [:asset-type [:= :video]]
      [:accessibility {:optional true} VideoAccessibilitySpec]
      [:start-seconds {:optional true} [:int {:min 0}]]
      [:variants {:optional true} [:vector {:min 1} VideoVariantSpec]]
      [:external {:optional true} ExternalSourceSpec]]]]
   [:image
    [:and
     BaseAssetSpec
     [:map
      [:asset-type [:= :image]]
      [:accessibility {:optional true} ImageAccessibilitySpec]
      [:variants {:optional true} [:vector {:min 1} ImageVariantSpec]]]]]])
