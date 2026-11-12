(ns mla.course-schema.exercise
  (:require [malli.util :as mu]
            [mla.course-schema.hiccup :as h]
            [mla.course-schema.import :as imp]
            [mla.course-schema.primitives :as p]))

(def ExerciseCommonSpec
  [:map
   [:id {:generated true} :uuid]
   [:annotate    {:optional true} [:vector [:maybe :keyword]]] ; per column, nil = none
   [:instruction {:optional true} h/InlineHiccupContent]
   [:hint        {:optional true} h/InlineHiccupContent]
   ;; canonical (model) version of the answer including punctuation marks,
   ;; when present used to display in UI as the correct answer.
   [:canonical   {:optional true} h/InlineHiccupContent]])

(def SentenceBuilderSentenceSpec
  "One sentence: `:words` in the correct order, every accepted ordering in
   `:answers`, and its own `:canonical` presentation."
  [:map
   [:words       [:vector {:min 1} h/TextOrRubyHiccup]]
   [:extra-words {:optional true} [:vector {:min 1} h/TextOrRubyHiccup]]
   [:answers     [:vector {:min 1} [:vector {:min 1} h/TextOrRubyHiccup]]]
   [:canonical   {:optional true} h/InlineHiccupContent]])

(def SentenceBuilderExerciseSpec
  (mu/merge
   ;; canonical belongs to each sentence, not to the exercise
   (mu/dissoc ExerciseCommonSpec :canonical)
   [:map
    [:type      [:= :sentence-builder]]
    [:sentences [:vector {:min 1} SentenceBuilderSentenceSpec]]]))

(def QuizOptionSpec
  [:map
   [:content h/InlineHiccupContent]
   [:correct :boolean]])

(defn- some-correct? [options] (boolean (some :correct options)))

(def ^:private at-least-one-correct
  [:fn {:error/message "at least one option must be correct"} some-correct?])

(def MultipleChoiceScoringSpec
  "How a multi-answer exercise is scored.
   :per-choice      — 1 point per correct option, minus 1 per wrong pick, floor 0 (default)
   :all-or-nothing  — 1 point only when exactly the correct set is selected"
  [:enum :per-choice :all-or-nothing])

(def MultipleChoiceExerciseSpec
  (mu/merge
   ExerciseCommonSpec
   [:map
    [:type    [:= :multiple-choice]]
    [:options [:and [:vector {:min 2 :max 16} QuizOptionSpec] at-least-one-correct]]
    [:scoring {:optional true} MultipleChoiceScoringSpec]]))

(def PairsExerciseSpec
  (mu/merge
   ExerciseCommonSpec
   [:map
    [:type  [:= :pairs]]
    [:pairs [:vector {:min 1} [:tuple h/InlineHiccupContent h/InlineHiccupContent]]]]))

(def SentenceOrderExerciseSpec
  (mu/merge
   ExerciseCommonSpec
   [:map
    [:type      [:= :sentence-order]]
    [:sentences [:vector {:min 2} h/InlineHiccupContent]]]))

(def TextSegmentSpec
  "A literal text run, shared by the cloze and placement exercises."
  [:map
   [:kind  [:= :text]]
   [:value h/TextOrRubyHiccup]])

(def ClozeBlankSegmentSpec
  [:map
   [:kind        [:= :blank]]
   [:answer      h/TextOrRubyHiccup]
   [:placeholder {:optional true} p/NonBlankStr]])

(def ClozeSegmentSpec
  [:multi {:dispatch :kind}
   [:text  TextSegmentSpec]
   [:blank ClozeBlankSegmentSpec]])

(def ClozeMultiExerciseSpec
  (mu/merge
   ExerciseCommonSpec
   [:map
    [:type     [:= :cloze-multi]]
    [:segments [:vector {:min 1} ClozeSegmentSpec]]]))

;; cloze-drag ─

(def ClozeDragSentenceSpec
  "One sentence: the same `:text`/`:blank` segment vector `:cloze-multi`
   uses for its whole passage, just scoped to a single sentence here so
   several can sit side by side in `:sentences`."
  [:vector {:min 1} ClozeSegmentSpec])

(def ClozeDragExerciseSpec
  (mu/merge
   ExerciseCommonSpec
   [:map
    [:type        [:= :cloze-drag]]
    [:sentences   [:vector {:min 1} ClozeDragSentenceSpec]]]))

;; --- Word Placement Exercise ---
(def PlacementGapSegmentSpec
  "A `^` decoy gap (`:answer-index` absent), or the correct gap for the
   nth entry of the sentence's `:words` list (1-based, matching
   authoring order in `^{n}`)."
  [:map
   [:kind          [:= :gap]]
   [:answer-index  {:optional true} [:int {:min 1}]]])

(def PlacementSegmentSpec
  [:multi {:dispatch :kind}
   [:text TextSegmentSpec]
   [:gap  PlacementGapSegmentSpec]])

(def WordPlacementSentenceSpec
  [:map
   [:segments [:vector {:min 1} PlacementSegmentSpec]]
   [:words    [:vector {:min 1} h/TextOrRubyHiccup]]])

(def WordPlacementExerciseSpec
  (mu/merge
   ExerciseCommonSpec
   [:map
    [:type        [:= :word-placement]]
    [:sentences   [:vector {:min 1} WordPlacementSentenceSpec]]]))

(def WordReorderSegmentSpec
  [:multi {:dispatch :kind}
   [:word        [:map [:kind [:= :word]] [:value h/TextOrRubyHiccup]]]
   [:punctuation [:map [:kind [:= :punctuation]] [:value h/TextOrRubyHiccup]]]])

(defn- word-reorder-answer-valid?
  "Every answer must be the same length as `segments`, agree with it
   value-for-value at every punctuation index (punctuation is
   positionally fixed), and be some permutation of `segments`' word
   values at the remaining (word) indices."
  [{:keys [segments answers]}]
  (let [n          (count segments)
        punct-at   (into {} (keep-indexed
                             (fn [i {:keys [kind value]}] (when (= kind :punctuation) [i value]))
                             segments))
        word-freqs (frequencies (keep #(when (= :word (:kind %)) (:value %)) segments))]
    (every? (fn [answer]
              (and (= n (count answer))
                   (every? (fn [[i v]] (= v (nth answer i))) punct-at)
                   (= word-freqs
                      (frequencies (keep-indexed #(when-not (contains? punct-at %1) %2) answer)))))
            answers)))

(def WordReorderSentenceSpec
  [:and
   [:map
    [:segments [:vector {:min 1} WordReorderSegmentSpec]]
    [:answers  [:vector {:min 1} [:vector {:min 1} h/TextOrRubyHiccup]]]]
   [:fn {:error/message "answers must keep punctuation fixed in place and permute only the words"}
    word-reorder-answer-valid?]])

(def WordReorderExerciseSpec
  (mu/merge
   ExerciseCommonSpec
   [:map
    [:type        [:= :word-reorder]]
    [:sentences   [:vector {:min 1} WordReorderSentenceSpec]]]))

(def ExerciseSpec
  [:multi {:dispatch :type}
   [:sentence-builder  SentenceBuilderExerciseSpec]
   [:multiple-choice   MultipleChoiceExerciseSpec]
   [:pairs             PairsExerciseSpec]
   [:sentence-order    SentenceOrderExerciseSpec]
   [:cloze-multi       ClozeMultiExerciseSpec]
   [:cloze-drag        ClozeDragExerciseSpec]
   [:word-placement    WordPlacementExerciseSpec]
   [:word-reorder      WordReorderExerciseSpec]])

(def ExerciseImportSpec
  (imp/->import-schema ExerciseSpec))
