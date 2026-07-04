(ns mla.course-schema.hiccup-policy
  "What course-authored hiccup may contain — one allow-list, as data, shared by
   the renderer optionally, the schema (use `allowed-tag?` / `allowed-attr?` in the
   `::hiccup` registry to *reject* instead)."
  (:require [clojure.string :as str]))

(def void-tags
  #{:area :base :br :col :embed :hr :img :input :link :meta :param :source :track :wbr})

;;; Tag groups ─

(def html-tags
  #{;; text
    :p :span :div :br :wbr :hr :em :strong :b :i :u :s :small :mark :sub :sup
    :del :ins :code :pre :kbd :samp :var :blockquote :q :cite :abbr :dfn :time
    :bdi :bdo :data
    ;; ruby (East-Asian annotations)
    :ruby :rt :rp :rb
    ;; structure
    :h1 :h2 :h3 :h4 :h5 :h6 :section :article :aside :header :footer :nav
    :figure :figcaption :details :summary
    ;; lists and tables
    :ul :ol :li :dl :dt :dd
    :table :caption :colgroup :col :thead :tbody :tfoot :tr :th :td
    ;; links and media
    :a :img :audio :video :source :track
    ;; embeds and drawing surfaces
    :iframe :canvas
    ;; inert controls (the app's TTS button / reveal checkboxes)
    :button :label :input})

(def svg-tags
  "`:title` is only honoured inside an <svg> (React 19 hoists a stray <title>
   to the document head)."
  #{:svg :g :path :circle :ellipse :rect :line :polyline :polygon
    :text :tspan :textPath :defs :symbol :marker :use :image
    :linearGradient :radialGradient :stop :clipPath :mask :pattern
    :animateTransform :animateMotion :title :desc})

(def math-tags
  #{:math :mi :mn :mo :ms :mtext :mrow :mfrac :msqrt :mroot :msub :msup
    :msubsup :munder :mover :munderover :mtable :mtr :mtd :mspace :mstyle
    :mpadded :menclose :semantics :annotation})

(def allowed-tags (into (into html-tags svg-tags) math-tags))

(def dropped-tags
  "Removed *with* their content. Every other unknown tag is unwrapped: its
   children render, the element itself doesn't.
   Not allowed, and why: script / style (code, CSS injection); object / embed /
   applet (load arbitrary plug-in documents — use a gated iframe); form,
   textarea, select, option (data collection / phishing); link, meta, base,
   html, head, body (document-level control); template, noscript, dialog,
   portal; SVG <foreignObject>, <animate>, <set> (HTML-in-SVG; can animate an
   href to a script URL)."
  #{:script :style :frame :frameset :object :embed :applet :template :noscript
    :link :meta :base :head :html :body :form :textarea :select :option
    :dialog :portal :fencedframe :foreignObject :animate :set})

(defn svg-tag?  [tag] (contains? svg-tags tag))
(defn math-tag? [tag] (contains? math-tags tag))
(defn foreign-tag?
  "SVG or MathML: attribute values get extra scrutiny (`url(...)`)."
  [tag]
  (or (svg-tag? tag) (math-tag? tag)))

;;; Attributes ─

(def global-attrs
  "Deliberately absent: :id outside SVG (DOM clobbering), :style (overlays,
   tracking), :role, :tabindex, every on* handler, :srcdoc, :srcset, :ref,
   :key, :dangerouslySetInnerHTML."
  #{:class :lang :dir :title})

(def svg-attrs
  "SVG-native spelling (hyphenated as in markup; the renderer camel-cases
   them for React). :id is allowed here — gradients, clip paths and markers
   need it — and is namespaced by the renderer."
  #{:id :viewBox :preserveAspectRatio :width :height :x :y :cx :cy :r :rx :ry
    :x1 :y1 :x2 :y2 :dx :dy :d :points :transform :fill :stroke :stroke-width
    :stroke-linecap :stroke-linejoin :stroke-dasharray :stroke-dashoffset
    :stroke-miterlimit :stroke-opacity :fill-opacity :fill-rule :clip-rule
    :opacity :font-size :font-family :font-weight :font-style :text-anchor
    :dominant-baseline :letter-spacing :offset :stop-color :stop-opacity
    :gradientUnits :gradientTransform :spreadMethod :patternUnits
    :patternTransform :clipPathUnits :maskUnits :markerWidth :markerHeight
    :refX :refY :orient :marker-start :marker-mid :marker-end :clip-path :mask
    :textLength :lengthAdjust :startOffset :href :xmlns
    ;; animateTransform / animateMotion
    :attributeName :type :values :dur :repeatCount :begin :from :to :path
    :keyPoints :rotate})

(def math-attrs
  #{:display :mathvariant :mathsize :mathcolor :mathbackground :fence :stretchy
    :separator :accent :accentunder :columnalign :rowalign :lspace :rspace
    :linethickness :displaystyle :scriptlevel :notation :width :height})

(def svg-ref-attrs
  "SVG attributes that may hold `url(#id)`; the only place `url(` is allowed."
  #{:fill :stroke :clip-path :mask :marker-start :marker-mid :marker-end})

(def fragment-href-tags
  "SVG tags whose :href may only point at an element in the same document."
  #{:use :textPath})

(def tag-attrs
  (merge
   {:a      #{:href :target}
    :img    #{:src :alt :width :height :loading}
    :audio  #{:src :controls :preload :loop :muted}
    :video  #{:src :controls :preload :loop :muted :poster :width :height :playsinline}
    :source #{:src :type}
    :track  #{:src :kind :srclang :label :default}
    :th     #{:colspan :rowspan :headers :scope}
    :td     #{:colspan :rowspan :headers}
    :col    #{:span}
    :colgroup #{:span}
    :ol     #{:start :reversed :type}
    :li     #{:value}
    :time   #{:datetime}
    :data   #{:value}
    :iframe #{:src :width :height :title :loading}
    :canvas #{:width :height}
    :button #{:type :disabled}
    :input  #{:type :name :checked :disabled}}
   (into {} (map (fn [t] [t svg-attrs])) svg-tags)
   (into {} (map (fn [t] [t math-attrs])) math-tags)))

(def url-attrs
  "Attributes whose value is a URL and must be checked before use."
  #{:href :src :poster})

(def allowed-input-types
  "`input` types course hiccup may use. Both are inert: no form exists to
   submit them, and the app delegates their events."
  #{"checkbox" "radio"})

(defn allowed-input-type?
  "True when `t` (a string, or nil when absent) is an allowed `input` type."
  [t]
  (contains? allowed-input-types t))

;;; Predicates ─

(defn allowed-tag? [tag]
  (contains? allowed-tags tag))

(defn allowed-attr?
  "True when `attr` may appear on `tag`. Namespaced keywords never may."
  [tag attr]
  (and (keyword? attr)
       (nil? (namespace attr))
       (or (contains? global-attrs attr)
           (contains? (get tag-attrs tag) attr)
           (let [n (name attr)]
             (or (str/starts-with? n "aria-")
                 (str/starts-with? n "data-"))))))
