(ns hireling.client-side-tests
  (:require [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-prot]
            [hireling.core :as hc]
            [clojure.walk :as walk]))


(def promise->chan-tests
  {:on    "promise->chan!"
   :tests [{:aspect  "returns a channel when given a promise"
            :test-fn (fn [is]
                       (let [test-value "It is 42"
                             test-promise (.resolve js/Promise test-value)
                             successful-chan (hc/promise->chan! test-promise)]

                         (is (satisfies? async-prot/ReadPort successful-chan))))}
           {:aspect  "returns a channel that actually reads"
            :test-fn (fn [is]
                       (let [test-value "It is 42"
                             test-promise (.resolve js/Promise test-value)
                             successful-chan (hc/promise->chan! test-promise)]
                         (let [equaltest (fn [a]
                                           (println "succeeds with a " a)
                                           (is (= test-value a)))]
                           (async/take! successful-chan equaltest))))}
           {:aspect  "returns a channel that deals with rejection"
            :test-fn (fn [is]
                       (let [reject-value "Because I said so."
                             reject-response-value {:type   :rejection
                                                    :reason reject-value}]
                         (let [equaltest (fn [a] (is (= reject-response-value a)))]
                           (async/take! (hc/promise->chan! (.reject js/Promise reject-value)) equaltest))))}]})

(def chan->promise-tests
  {:on "chan->promise!"
   :tests [{:aspect "returns a promise"
            :test-fn (fn [is]
                       (let [test-chan (async/chan)
                             converted-promise (hc/chan->promise! test-chan)
                             should-equal-converted (.resolve js/Promise converted-promise)]
                         (is (= converted-promise should-equal-converted))))}
           {:aspect "returns a promise that correctly resolves"
            :test-fn (fn [is]
                       (let [test-chan (async/chan)
                             converted-promise (hc/chan->promise! test-chan)
                             test-data "Answer is 42"]
                         (.. converted-promise
                              (then (fn [a]
                                      (is (= test-data a)))))
                         (async/go (async/>! test-chan test-data))))}]})

(def chan->promise->chan-tests
  {:on "chan->promise! and promise->chan!"
   :tests [{:aspect "go back and forth without loss"
            :test-fn (fn [is]
                       (let [starting-chan (async/chan)
                             test-data "MAN was that tunnel dark!"]
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
                             (hc/promise->chan!)
                             (hc/chan->promise!)
                             (hc/promise->chan!)
                             (hc/chan->promise!)
                             (hc/promise->chan!)
                             (hc/chan->promise!)
                             (hc/promise->chan!)
                             (hc/chan->promise!)
                             (hc/promise->chan!)
                             (hc/chan->promise!)
                             (.then (fn [a] (is (= test-data a)))))
                         (async/go (async/>! starting-chan test-data))))}]})



(def all-tests
  [promise->chan-tests
   chan->promise-tests
   chan->promise->chan-tests])