(ns mla.course-schema.hiccup
  (:require [mla.course-schema.hiccup-policy :as policy]
            [mla.course-schema.primitives :as p]))

(def Hiccup
  [:schema
   {:registry
    {::hiccup [:orn
               [:node [:catn
                       [:name [:fn {:error/message "tag not allowed"} policy/allowed-tag?]]
                       [:props [:? [:map-of keyword? any?]]]
                       [:children [:* [:schema [:ref ::hiccup]]]]]]
               ;; TODO narrow and fix [:exercise [:schema [:ref :schema/exercise]]]
               [:exercise [:map [:type keyword?]]]
               [:primitive [:orn
                            [:nil nil?]
                            [:boolean boolean?]
                            [:number number?]
                            [:text string?]]]]}}
   ::hiccup])

(defn- tagged-text-catn [tag]
  [:catn
   [:tag   [:= tag]]
   [:attrs [:? [:map-of keyword? :any]]]
   [:text  p/NonBlankStr]])

(def RtHiccup   (tagged-text-catn :rt))
(def RpHiccup   (tagged-text-catn :rp))
(def SpanHiccup (tagged-text-catn :span))

(def RubyBaseHiccup
  "A ruby's base text: a bare string, or a `SpanHiccup` when the base
   itself needs its own attributes (e.g. `:lang`) rather than relying
   on `:ruby`'s own top-level ones."
  [:or p/NonBlankStr [:schema SpanHiccup]])

(def RubyHiccup
  "`:ruby` tag, optional attrs, a base followed by any sequence of :rt and :rp annotations"
  [:catn
   [:tag   [:= :ruby]]
   [:attrs [:? [:map-of keyword? :any]]]
   [:base  [:schema RubyBaseHiccup]]
   [:annotations [:* [:or
                      [:schema RtHiccup]
                      [:schema RpHiccup]]]]])

(def TextOrRubyHiccup
  "Single non empty string or Ruby hiccup element"
  [:or p/NonBlankStr [:schema RubyHiccup]])

(def InlineHiccupContent
  "Represents inline text content as a sequence consisting of text node or inline tag node."
  [:schema
   {:registry
    {::node [:orn
             [:text    string?]
             [:element [:catn
                        [:tag      keyword?]
                        [:attrs    [:? [:map-of keyword? :any]]]
                        [:children [:* [:schema [:ref ::node]]]]]]]}}
   [:vector [:schema [:ref ::node]]]])
