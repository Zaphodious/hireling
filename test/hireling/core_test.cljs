(ns hireling.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-prot]
            [hireling.core :as hc]
            [cljs.test :as test]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(deftest promise-wrapper
  (let [test-value "It is 42"
        reject-value "Because I said so."
        test-promise (.resolve js/Promise test-value)
        successful-chan (hc/wrap-promise! {:promise test-promise})]
    (testing "That wrap-promise returns a channel when given a promise."
      (is (satisfies? async-prot/ReadPort successful-chan)))
    (testing "That the channel returned by wrap-promise gets the proper value when resolved"
      (test/async done
        (let [equaltest (fn [a] (is (= test-value a))
                          (done))]
          (async/take! successful-chan equaltest))))
    (testing "That the channel returned by wrap-promise gets the proper error message when rejected"
      (test/async done
        (let [equaltest (fn [a] (is (= reject-value a))
                          (done))]
          (async/take! (hc/wrap-promise! {:promise (.reject js/Promise reject-value)}) equaltest))))))
