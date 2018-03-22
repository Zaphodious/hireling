(ns hireling.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [hireling.core :as hc]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(deftest promise-converter
  (testing "Converter that takes a js promise and turns it into a core.async channel."
    (is (let [a (js/Promise.)]
          (hc/wrap-promise {:promise a})))))