(ns hireling.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-prot]
            [hireling.core :as hc]
            [cljs.test :as test]
            [clojure.walk :as walk]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(deftest promise-wrapper-chan?
  (testing "That wrap-promise returns a channel when given a promise."
    (let [test-value "It is 42"
          test-promise (.resolve js/Promise test-value)
          successful-chan (hc/promise->chan! test-promise)]

      (is (satisfies? async-prot/ReadPort successful-chan)))))

(deftest promise-wrapper-reads
  (testing "That the channel returned by wrap-promise gets the proper value when resolved"
    (let [test-value "It is 42"
          test-promise (.resolve js/Promise test-value)
          successful-chan (hc/promise->chan! test-promise)]
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
          (async/take! (hc/promise->chan! (.reject js/Promise reject-value)) equaltest))))))

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

(deftest cache-initializer-returns-chan
  (testing "That the cache initializer returns a channel."
    (is (satisfies? async-prot/ReadPort (hc/open-cache "Hi")))))

(deftest cache-initializers-chan-gets-a-cache
  (testing "That the channel returned by open-cache gets a cache object."
    (test/async done
      (async/go
        (let [cache? (async/<! (hc/open-cache "Hi!"))]
          (println "the thing bot back is " cache?)
          (is (and (.put cache?)
                   (.match cache?)
                   (.matchAll cache?)))
          (done))))))

(deftest header-converter-makes-round-trip
  (testing "That the header converter functions are reversible"
    (let [test-headers {:foo "too"
                        :this "that"
                        :answer "42"}
          converted (hc/map->headers test-headers)
          round-tripped (hc/headers->map converted)]
      (is (= test-headers round-tripped)))))

(deftest request-converter-gets-a-map
  (testing "That request->map returns a map with the relevant properties expressed in EDN"
    (let [test-url "https://www.google.com/"
          test-method "GET"
          requi (js/Request. test-url)
          req-map (hc/request->map requi)]
      (is (and (= (:url req-map) test-url)
               (= (:method req-map) test-method))))))

(deftest map-to-request-returns-proper-request
  (testing "That map->request returns a request with the specified properties."
    (let [test-map {:url "https://www.google.com/"
                    :method "PUT"
                    :headers {:foo "too"}
                    :cache "no-cache"}
          requi (hc/map->request test-map)]
      (println "headers are " (walk/keywordize-keys (into {} (map vec (es6-iterator-seq (.entries (.-headers requi)))))))
      (is (and (= (.-url requi) (:url test-map))
               (= (.-method requi) (:method test-map))
               (= (.-cache requi) (:cache test-map))
               (= (hc/headers->map (.-headers requi))
                  (:headers test-map)))))))

(deftest request-converter-functions-round-trip
  (testing "That map->request and request->map are reversible"
    (let [initial-request (js/Request.
                            "https://www.google.com/"
                            (clj->js {:method "PUT" :cache "reload"
                                      :headers {:thing "hello"
                                                :content-type "text/html;charset=UTF-8"}}))
          round-tripped-request (-> initial-request
                                    (hc/request->map)
                                    (hc/map->request)
                                    (hc/request->map)
                                    (hc/map->request))]
      (is (= (hc/request->map initial-request)
             (hc/request->map round-tripped-request))))))

(deftest response-converter-returns-map
  (testing "That response->map returns a map"
    (is (map? (hc/response->map! (js/Response. (pr-str {:thing "another" :answer 42})))))))

(deftest response-converter-captures-correct-data
  (testing "That response->map's map contain correct values"
    (let [sample-status-text "Passed!"
          sample-status 200
          sample-body (pr-str {:thing "another" :answer 42})
          sample-headers {:foo "bar"
                          :thing "another"}
          sample-init {:statusText sample-status-text
                       :status sample-status
                       :foo "bar"
                       :headers sample-headers}
          converted-map (hc/response->map! (js/Response.
                                             sample-body (clj->js sample-init)))]
      (is (or (= (:status converted-map) sample-status)
              (= (:status-text converted-map) sample-status-text)
              (= (:headers sample-headers)))))))