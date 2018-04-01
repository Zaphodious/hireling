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
           {:aspect    "returns a channel that deals with rejection"
            :should-be {::hc/rejection "Because I said so."}
            :test-fn   (fn [is]
                         (let [reject-value "Because I said so."]
                           (let [equaltest (fn [a] (is a))]
                             (async/take! (hc/promise->chan! (.reject js/Promise reject-value)) equaltest))))}]})

(def chan->promise-tests
  {:on    "chan->promise!"
   :tests [{:aspect  "returns a promise"
            :test-fn (fn [is]
                       (let [test-chan (async/chan)
                             converted-promise (hc/chan->promise! test-chan)
                             should-equal-converted (.resolve js/Promise converted-promise)]
                         (is (= converted-promise should-equal-converted))))}
           {:aspect  "returns a promise that correctly resolves"
            :test-fn (fn [is]
                       (let [test-chan (async/chan)
                             converted-promise (hc/chan->promise! test-chan)
                             test-data "Answer is 42"]
                         (.. converted-promise
                             (then (fn [a]
                                     (is (= test-data a)))))
                         (async/go (async/>! test-chan test-data))))}
           {:aspect    "returns a promise that correctly rejects"
            :should-be "That's what the doctor ordered."
            :test-fn   (fn [is]
                         (let [test-chan (async/chan)
                               converted-promise (hc/chan->promise! test-chan)
                               failtest (fn [a] (is "The promise didn't reject!"))
                               equaltest (fn [a] (is a))]
                           (.. converted-promise
                               (then
                                 failtest
                                 equaltest))
                           (async/put! test-chan {::hc/rejection "That's what the doctor ordered."})))}]})


(def chan->promise->chan-tests
  {:on    "chan->promise! and promise->chan!"
   :tests [{:aspect  "go back and forth without loss"
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
                         (async/go (async/>! starting-chan test-data))))}
           {:aspect    "fail cascades correctly."
            :should-be "I hit every branch on the way down."
            :test-fn   (fn [is]
                         (let [starting-chan (async/chan)
                               test-data "I hit every branch on the way down."
                               fail-fn (fn [a] (is "The promise/chan stack didn't reject."))
                               pass-fn #(is %)]
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
                               (.then
                                 fail-fn
                                 pass-fn))
                           (async/go (async/>! starting-chan {::hc/rejection test-data}))))}]})

(def header-converter-tests
  {:on    "map->headers and headers->map"
   :tests [{:aspect  "are reversible"
            :test-fn (fn [is]
                       (let [test-headers {:foo    "too"
                                           :this   "that"
                                           :answer "42"}
                             converted (hc/map->headers test-headers)
                             round-tripped (hc/headers->map converted)]
                         (is (= test-headers round-tripped))))}]})

(def request->map-tests
  {:on    "request->map"
   :tests [{:aspect  "returns a map with the relevant properties expressed in EDN"
            :test-fn (fn [is]
                       (let [test-url "https://www.google.com/"
                             test-method "GET"
                             requi (js/Request. test-url)
                             req-map (hc/request->map requi)]
                         (is (and (= (:url req-map) test-url)
                                  (= (:method req-map) test-method)))))}]})

(def map->request-tests
  {:on    "map->request"
   :tests [{:aspect  "returns a request with the specified properties"
            :test-fn (fn [is]
                       (let [test-map {:url     "https://www.google.com/"
                                       :method  "PUT"
                                       :headers {:foo "too"}
                                       :cache   "no-cache"}
                             requi (hc/map->request test-map)]
                         (println "headers are " (walk/keywordize-keys (into {} (map vec (es6-iterator-seq (.entries (.-headers requi)))))))
                         (is (and (= (.-url requi) (:url test-map))
                                  (= (.-method requi) (:method test-map))
                                  (= (.-cache requi) (:cache test-map))
                                  (= (hc/headers->map (.-headers requi))
                                     (:headers test-map))))))}]})

(def map->request->map-tests
  {:on    "map->request and request->map"
   :tests [{:aspect  "are reversible"
            :test-fn (fn [is]
                       (let [initial-request (js/Request.
                                               "https://www.google.com/"
                                               (clj->js {:method  "PUT" :cache "reload"
                                                         :headers {:thing        "hello"
                                                                   :content-type "text/html;charset=UTF-8"}}))
                             round-tripped-request (-> initial-request
                                                       (hc/request->map)
                                                       (hc/map->request)
                                                       (hc/request->map)
                                                       (hc/map->request))]
                         (is (= (hc/request->map initial-request)
                                (hc/request->map round-tripped-request)))))}]})

(def response->map-tests
  {:on    "response->map"
   :tests [{:aspect  "returns a map"
            :test-fn (fn [is]
                       (is (map? (hc/response->map! (js/Response. (pr-str {:thing "another" :answer 42}))))))}
           {:aspect  "returns a map that contains right values"
            :test-fn (fn [is]
                       (let [sample-status-text "Passed!"
                             sample-status 200
                             sample-body (pr-str {:thing "another" :answer 42})
                             sample-headers {:foo   "bar"
                                             :thing "another"}
                             sample-init {:statusText sample-status-text
                                          :status     sample-status
                                          :foo        "bar"
                                          :headers    sample-headers}
                             converted-map (hc/response->map! (js/Response.
                                                                sample-body (clj->js sample-init)))]
                         (is (and (= (:status converted-map) sample-status)
                                  (= (:status-text converted-map) sample-status-text)
                                  (= (:headers sample-headers))))))}]})

(def map->response-tests
  {:on    "map->request"
   :tests [{:aspect  "constructs a response correctly"
            :test-fn (fn [is]
                       (let [sample-status-text "Yes, did it"
                             sample-status 201
                             sample-body (pr-str {:whatever "floats" :your [\b \o \a \t]})
                             sample-param {:statusText sample-status-text
                                           :status     sample-status
                                           :body       sample-body}
                             constructed-response (hc/map->response! sample-param)]
                         (println "response is " constructed-response)
                         (is (and (= (.-status constructed-response) sample-status)))))}]})

(def simple-text-url "/simple.txt")
(def never-cache-url "/rand/never-cached.txt")
(def always-cache-url "/rand/always-cached.txt")
(def cache-updates-url "/rand/cache-updates.txt")

(defn fetch-equiv-test
  "Takes the assertion function, the url to call, and the number of times the url should be called."
  [is sample-url samples]
  (let [uni-chan (async/chan samples)
        fetch-it-fn (fn [] (-> (js/fetch sample-url) (.then #(.text %)) (.then #(async/put! uni-chan %))))
        reduced-chan (async/into #{} (async/take samples uni-chan))]
    (doall (map fetch-it-fn (range 0 samples)))
    (async/go
      (let [the-set (async/<! reduced-chan)]
        (println "the set is " the-set)
        (is (count the-set))))))

(def service-worker-caching-tests
  {:on    "Service Worker Networking"
   :tests [{:aspect       "correctly passes non-cached data through."
            :testing-args [never-cache-url 30]
            :should-be    30                                ;assures that we are, in fact, getting a substantial amount of data through.
            :test-fn      fetch-equiv-test}
           {:aspect       "correctly caches data"
            :testing-args [always-cache-url 30]
            :should-be    1                                 ;assures that all the data received is identical.
            :test-fn      fetch-equiv-test}
           {:aspect       "gets the fastest response between cache and server. Always fails. Set simulated ping between 15 and 30 to see differences."
            :testing-args [cache-updates-url 30]
            :should-be    29                                ;assures that all the data received is identical.
            :test-fn      fetch-equiv-test}]})


;(def fetch-tests
;  {:on    "hireling.core/fetch"
;   :tests [{:aspect       "properly returns a chan."
;            :testing-args [simple-text-url]
;            :test-fn      (fn [is stu]
;                            (is (satisfies? async-prot/ReadPort (hc/fetch {:url stu}))))}
;           {:aspect       "chan gets a response map if no return-type is specified."
;            :should-be    {:status-text "OK" :type "basic" :status 200}
;            :testing-args [simple-text-url]
;            :test-fn      (fn [is stu]
;                            (async/take! (hc/fetch {:url stu})
;                                         (fn [a] (is (select-keys a [:status-text :type :status])))))}
;           {:aspect       "chan gets text if the return-type is :string"
;            :should-be    "I'm from a server!"
;            :testing-args [simple-text-url]
;            :test-fn      (fn [is stu]
;                            (async/take! (hc/fetch {:url stu :return-type :string})
;                                         (fn [a] (is a))))}]})


(def all-tests
  [service-worker-caching-tests
   map->response-tests
   response->map-tests
   map->request->map-tests
   map->request-tests
   request->map-tests
   header-converter-tests
   chan->promise->chan-tests
   promise->chan-tests
   chan->promise-tests])