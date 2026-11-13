(ns mla.course-schema.hiccup-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [mla.course-schema.hiccup :as sut]))

(deftest title-hiccup-content-test
  (testing "plain text and ruby are valid"
    (is (m/validate sut/TitleHiccupContent
                    ["Lesson 1 " [:ruby [:span "你好"] [:rp " ("] [:rt "nǐhǎo"] [:rp ")"]]])))
  (testing "any other element is rejected"
    (is (not (m/validate sut/TitleHiccupContent ["Lesson " [:strong "1"]])))))
