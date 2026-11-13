(ns mla.course-schema.lang-block-test
  (:require [clojure.test :refer [deftest is]]
            [malli.core :as m]
            [mla.course-schema.lang-block :as sut]))

(deftest lang-block-defaults-test
  (is (m/validate sut/LangBlockDefaultsSpec {}))
  (is (m/validate sut/LangBlockDefaultsSpec
                  {:lang-block-languages   [{:label :zh :langs [:zh-Hans :zh-Latn]}
                                            {:label :en :langs [:en]}]
                   :lang-block-translation :en
                   :lang-block-annotate    {:zh-Hans :pinyin}
                   :ruby-languages         [:zh-Hans :zh-Latn :en]}))
  (is (not (m/validate sut/LangBlockDefaultsSpec {:lang-block-translation "en"})))
  (is (not (m/validate sut/LangBlockDefaultsSpec {:lang-block-languages []}))))
