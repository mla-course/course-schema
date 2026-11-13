(ns mla.course-schema.exercise-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.pprint :as pp]
            [malli.core :as m]
            [malli.error :as me]
            [mla.course-schema.exercise :as sut]))

(defn- id [] #uuid "cc59006b-6df8-48c5-a1b1-2ebcf5058568")

(defn- invalid?
  "True when `data` does NOT satisfy `spec`."
  [spec data]
  (let [valid? (m/validate spec data)]
    (when valid?
      (some-> (m/explain spec data) me/humanize pp/pprint))
    (not valid?)))

(defn- text [s] [s])

(defn- ruby-text [base & annotations]
  [(into [:ruby base] (map (fn [a] [:rt a])) annotations)])

(defn- image
  ([src] (image src nil))
  ([src alt] [[:img (cond-> {:src src} alt (assoc :alt alt))]]))

(defn- audio [src]
  [[:audio {:controls "" :src src}]])

(defn- option [content correct?]
  {:content content :correct correct?})

(defn- ruby-node [base & annotations]
  (into [:ruby base] (map (fn [a] [:rt a])) annotations))

(deftest sentence-builder-schema-test
  (testing "a minimal, valid exercise"
    (is (m/validate sut/SentenceBuilderExerciseSpec
                    {:type :sentence-builder
                     :id (id)
                     :sentences [{:words ["I" "like" "cats"]
                                  :answers [["I" "like" "cats"]]}]})))

  (testing "a rich exercise: ruby-annotated word, multiple answers, canonical, and markup"
    (is (m/validate sut/SentenceBuilderExerciseSpec
                    {:type :sentence-builder
                     :id (id)
                     :instruction ["Arrange the words to form a " [:strong "correct"] " sentence."]
                     :sentences   [{:words       ["我" "喜欢" (ruby-node "猫" "māo")]
                                    :extra-words ["狗"]
                                    :answers     [["我" "喜欢" (ruby-node "猫" "māo")]
                                                  [(ruby-node "猫" "māo") "我" "喜欢"]]
                                    :canonical   ["我喜欢猫。"]}]
                     :hint        [(ruby-node "喜欢" "xǐhuān") " means \"to like\""]})))

  (testing ":annotate validation"
    (is (m/validate sut/SentenceBuilderExerciseSpec
                    {:type :sentence-builder :id (id)
                     :annotate [:pinyin nil]
                     :sentences [{:words ["我"] :answers [["我"]]}]}))
    (is (invalid? sut/SentenceBuilderExerciseSpec
                  {:type :sentence-builder :id (id)
                   :annotate ["pinyin"]
                   :sentences [{:words ["我"] :answers [["我"]]}]})))

  (testing "invalid: empty answers or empty word-order vector"
    (is (invalid? sut/SentenceBuilderExerciseSpec
                  {:type :sentence-builder :id (id)
                   :sentences [{:words ["I" "like" "cats"]
                                :answers []}]}))
    (is (invalid? sut/SentenceBuilderExerciseSpec
                  {:type :sentence-builder :id (id)
                   :sentences [{:words ["I"]
                                :answers [[]]}]}))))

(deftest multiple-choice-schema-test
  (testing "a minimal, valid exercise"
    (is (m/validate sut/MultipleChoiceExerciseSpec
                    {:type :multiple-choice :id (id)
                     :options [(option (text "Casa") true)
                               (option (text "Perro") false)]})))

  (testing "a rich exercise with mixed options and instructions"
    (is (m/validate sut/MultipleChoiceExerciseSpec
                    {:type :multiple-choice :id (id)
                     :instruction ["Select the correct translation for " [:strong "\"Gato\""] "."]
                     :options [(option (image "cat.jpg" "A cat") true)
                               (option (ruby-text "狗" "gǒu") false)
                               (option (text "Casa") false)]})))

  (testing "invalid option counts or types"
    (is (invalid? sut/MultipleChoiceExerciseSpec
                  {:type :multiple-choice :id (id)
                   :options [(option (text "Casa") true)]}))
    (is (invalid? sut/MultipleChoiceExerciseSpec
                  {:type :multiple-choice :id (id)
                   :options (vec (repeat 17 (option (text "x") false)))}))))

(deftest pairs-schema-test
  (testing "valid text and mixed content pairs"
    (is (m/validate sut/PairsExerciseSpec
                    {:type :pairs :id (id)
                     :pairs [[(text "House") (text "Casa")]
                             [(text "Dog") (text "Perro")]]}))
    (is (m/validate sut/PairsExerciseSpec
                    {:type :pairs :id (id)
                     :pairs [[(text "House") (image "house.jpg" "A house")]
                             [(text "Dog")   (ruby-text "狗" "gǒu")]
                             [(text "Bark")  (audio "bark.mp3")]]})))

  (testing "invalid pair structure"
    (is (invalid? sut/PairsExerciseSpec
                  {:type :pairs :id (id)
                   :pairs [[(text "House") (text "Casa") (text "extra")]]}))))

(deftest sentence-order-schema-test
  (testing "valid sentence order exercises"
    (is (m/validate sut/SentenceOrderExerciseSpec
                    {:type :sentence-order :id (id)
                     :sentences [(text "I woke up.")
                                 (text "Then I went to school.")]})))

  (testing "invalid sentence count or element structure"
    (is (invalid? sut/SentenceOrderExerciseSpec
                  {:type :sentence-order :id (id) :sentences [(text "Only one.")]}))
    (is (invalid? sut/SentenceOrderExerciseSpec
                  {:type :sentence-order :id (id)
                   :sentences ["I woke up." "Then I went to school."]}))))

(deftest cloze-multi-schema-test
  (testing "valid cloze-multi exercise"
    (is (m/validate sut/ClozeMultiExerciseSpec
                    {:type :cloze-multi
                     :id (id)
                     :hint ["Remember the " [:strong "measure word"] "."]
                     :segments [{:kind :text :value "我"}
                                {:kind :text :value (ruby-node "喜欢" "xǐhuān")}
                                {:kind :blank :answer "猫" :placeholder "___"}]}))))

(deftest cloze-drag-schema-test
  (testing "valid cloze-drag exercise"
    (is (m/validate sut/ClozeDragExerciseSpec
                    {:type :cloze-drag
                     :id (id)
                     :instruction ["Fill in each blank."]
                     :sentences [[{:kind :text :value "我想买一张"}
                                  {:kind :blank :answer "手机"}
                                  {:kind :text :value "卡。"}]]}))))

(deftest word-placement-schema-test
  (testing "valid word-placement exercise"
    (is (m/validate sut/WordPlacementExerciseSpec
                    {:type :word-placement :id (id)
                     :instruction ["Place the words in the correct gaps."]
                     :sentences [{:segments [{:kind :text :value "I like "}
                                             {:kind :gap}
                                             {:kind :gap :answer-index 1}
                                             {:kind :text :value "."}]
                                  :words ["dogs"]}]}))))

(deftest word-reorder-schema-test
  (testing "valid word-reorder exercise"
    (is (m/validate sut/WordReorderExerciseSpec
                    {:type :word-reorder :id (id)
                     :instruction ["Put the words in the right order."]
                     :sentences [{:segments [{:kind :word :value "I"}
                                             {:kind :word :value "like"}
                                             {:kind :punctuation :value "."}]
                                  :answers [["I" "like" "."]]}]}))))

(def ^:private exercise-fixtures
  {:sentence-builder  {:type :sentence-builder :id (id)
                       :sentences [{:words ["I" "like" "cats"]
                                    :answers [["I" "like" "cats"]]}]}
   :multiple-choice   {:type :multiple-choice :id (id)
                       :options [(option (text "Casa") true)
                                 (option (text "Perro") false)]}
   :pairs             {:type :pairs :id (id)
                       :pairs [[(text "House") (text "Casa")]]}
   :sentence-order    {:type :sentence-order :id (id)
                       :sentences [(text "I woke up.")
                                   (text "Then I went to school.")]}
   :cloze-multi       {:type :cloze-multi :id (id)
                       :segments [{:kind :text :value "我"}
                                  {:kind :blank :answer "猫"}]}
   :cloze-drag        {:type :cloze-drag :id (id)
                       :sentences [[{:kind :text :value "我"}
                                    {:kind :blank :answer "猫"}]]}
   :word-placement    {:type :word-placement :id (id)
                       :sentences [{:segments [{:kind :gap :answer-index 1}]
                                    :words ["black"]}]}
   :word-reorder      {:type :word-reorder :id (id)
                       :sentences [{:segments [{:kind :word :value "cats"}]
                                    :answers [["cats"]]}]}})

(deftest exercise-fixtures-cover-every-dispatch-branch-test
  (testing "fixture map and ExerciseSpec's multi-dispatch branches stay in sync"
    (is (= (set (keys exercise-fixtures))
           (set (map first (m/children sut/ExerciseSpec)))))))

(deftest exercise-spec-dispatch-test
  (testing "ExerciseSpec dispatches on :type correctly for every branch"
    (doseq [[ex-type fixture] exercise-fixtures]
      (testing ex-type
        (is (m/validate sut/ExerciseSpec fixture)))))
  (testing "invalid: unrecognized :type"
    (is (invalid? sut/ExerciseSpec {:type :flashcard :id (id)}))))
