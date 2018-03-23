(ns hireling.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-prot]
            [hireling.core :as hc]
            [cljs.test :as test]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(deftest promise-wrapper-chan?
  (testing "That wrap-promise returns a channel when given a promise."
    (let [test-value "It is 42"
          test-promise (.resolve js/Promise test-value)
          successful-chan (hc/promise->chan! {:promise test-promise})]

      (is (satisfies? async-prot/ReadPort successful-chan)))))

(deftest promise-wrapper-reads
  (testing "That the channel returned by wrap-promise gets the proper value when resolved"
    (let [test-value "It is 42"
          test-promise (.resolve js/Promise test-value)
          successful-chan (hc/promise->chan! {:promise test-promise})]
      (test/async done
        (let [equaltest (fn [a]
                          (println "succeeds with a " a)
                          (is (= test-value a))
                          (done))]
          (async/take! successful-chan equaltest))))))

(deftest promise-wrapper-handles-errors
  (testing "That the channel returned by promise->chan! gets the proper error message when rejected"
    (let [reject-value "Because I said so."
          reject-response-value {:type :rejection
                                 :reason reject-value}]
      (test/async done
        (let [equaltest (fn [a] (is (= reject-response-value a))
                          (done))]
          (async/take! (hc/promise->chan! {:promise (.reject js/Promise reject-value)}) equaltest))))))

(deftest chan-to-promise-returns-promise
  (testing "That chan->promise! returns a promise"
    (let [test-chan (async/chan)
          converted-promise (hc/chan->promise! test-chan)
          should-equal-converted (.resolve js/Promise converted-promise)]
      (is (= converted-promise should-equal-converted)))))

(deftest chan-to-promise-correctly-resolves
  (testing "That the promise returned by chan->promise! correctly resolves the thing put into the chan."
    (let [test-chan (async/chan)
          converted-promise (hc/chan->promise! test-chan)
          test-data "Answer is 42"]
      (test/async done
        (.. converted-promise
            (then (fn [a]
                    (is (= test-data a))
                    (done))))
        (async/go (async/>! test-chan test-data))))))

(deftest promise-chan-round-trip
  (testing "That the promise/chan converters go back and forth without signal loss."
    (let [starting-chan (async/chan)
          test-data "MAN was that tunnel was dark!"]
      (test/async done
        (-> starting-chan
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (hc/promise->chan!)
            (hc/chan->promise!)
            (.then (fn [a] (is (= test-data a)
                               (done))))))
      (async/go (async/>! starting-chan test-data)))))