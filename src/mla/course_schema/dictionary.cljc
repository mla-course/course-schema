(ns mla.course-schema.dictionary
  (:require [clojure.set :as set]
            [mla.course-schema.primitives :as p]
            [mla.course-schema.hiccup :as h]))

(def PartOfSpeech
  [:enum
   :noun :proper-noun :verb :adjective :adverb :pronoun :determiner :article
   :preposition :postposition :conjunction :interjection :particle :numeral
   :complement :classifier :symbol :punctuation :phrase])

(def part-of-speech-subtypes
  {:pronoun    #{:demonstrative :personal :possessive :interrogative :relative
                 :reflexive :reciprocal :indefinite}
   :determiner #{:demonstrative :possessive :interrogative :indefinite}
   :article    #{:definite :indefinite}
   :noun       #{:common :collective :mass :countable}
   :verb       #{:transitive :intransitive :ditransitive
                 :auxiliary :modal :copula :linking :phrasal :stative}
   :numeral    #{:cardinal :ordinal}
   :adverb     #{:manner :degree :frequency :temporal :locative}
   :phrase     #{}})

(def PartOfSpeechSubtype
  "Every subtype allowed for some part of speech."
  (into [:enum] (sort (apply set/union (vals part-of-speech-subtypes)))))

(defn part-of-speech-subtypes-valid?
  [assignment]
  (let [[pos subtypes] (if (vector? assignment) assignment [assignment nil])
        allowed (get part-of-speech-subtypes pos #{})]
    (every? allowed (or subtypes #{}))))

(def PartOfSpeechAssignmentSpec
  [:and
   [:or PartOfSpeech [:tuple PartOfSpeech [:set PartOfSpeechSubtype]]]
   [:fn {:error/message "Invalid part of speech subtype combination"}
    part-of-speech-subtypes-valid?]])

(def SenseChildSpec
  [:map
   [:title   {:optional true} p/NonBlankStr]
   [:key     {:optional true} :keyword]
   [:content :any]])

(def SenseSpec
  [:map
   [:parts-of-speech {:optional true}
    [:vector {:min 1} PartOfSpeechAssignmentSpec]]
   [:definition p/NonBlankStr]
   [:children {:optional true} [:vector SenseChildSpec]]])

(def ChineseSenseSpec
  [:and
   SenseSpec
   [:map
    [:classifiers {:optional true} [:vector :string]]]])

(def EntrySpec
  [:map
   [:lexeme [:string {:description "The canonical headword or lemma"}]]
   [:senses {:optional true} [:vector SenseSpec]]
   [:asset-ids {:optional true} [:vector :uuid]]
   [:category {:optional true} :keyword]
   [:tags     {:optional true} [:vector :keyword]]
   [:notes {:optional true} [:vector h/Hiccup]]])

(def LanguageEntrySpec
  [:and
   EntrySpec
   [:map
    {:title "Language Entry"
     :description "A language-agnostic dictionary entry for upstream sourcing."
     :example {:lexeme "你好"}}]])

(def ChineseEntrySpec
  [:and
   LanguageEntrySpec
   [:map
    [:pinyin {:optional true} :string]
    [:traditional {:optional true} :string]
    [:senses {:optional true} [:vector ChineseSenseSpec]]]])
