(ns hireling.client-side-tests
  (:require [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-prot]
            [hireling.core :as hc]
            [clojure.walk :as walk]
            [hireling.routes :as hroutes]
            [bidi.bidi :as bidi]))



(def simple-text-url (bidi/path-for hroutes/routemap ::hroutes/simple-txt))
(def never-cache-url (bidi/path-for hroutes/routemap ::hroutes/never-cache-txt))
(def always-cache-url (bidi/path-for hroutes/routemap ::hroutes/always-cache-txt))
(def fastest-cache-url (bidi/path-for hroutes/routemap ::hroutes/fastest-cache-txt))
(def rand-all-url-base (bidi/path-for hroutes/routemap ::hroutes/rand-all))

(defn fetch-equiv-test
  "Takes the assertion function, the url to call, and the number of times the url should be called."
  [is sample-url samples]
  (let [uni-chan (async/chan samples)
        fetch-it-fn (fn [] (-> (js/fetch sample-url) (.then #(.text %)) (.then #(async/put! uni-chan %))))
        reduced-chan (async/into #{} (async/take samples uni-chan))]
    (doall (map fetch-it-fn (range 0 samples)))
    (async/go
      (let [the-set (async/<! reduced-chan)]
        (is (count the-set))))))

(def service-worker-cache-paths-test
  {:on    "Service Worker :cache-routes "
   :tests [{:aspect       "correctly passes non-cached data through."
            :testing-args [never-cache-url 30]
            :should-be    30                                ;assures that we are, in fact, getting a substantial amount of data through.
            :test-fn      fetch-equiv-test}
           {:aspect "also for function routes"
            :testing-args [(str rand-all-url-base "uncached/" (gensym "never"))
                           30]
            :should-be 30
            :test-fn fetch-equiv-test}
           {:aspect       "correctly caches :cache-first string routes"
            :testing-args [always-cache-url 30]
            :should-be    1                                 ;assures that all the data received is identical.
            :test-fn      fetch-equiv-test}
           {:aspect       "correctly caches :cache-first function routes"
            :testing-args [(str rand-all-url-base "allcached/" (gensym "always")) 30]
            :should-be    1                                 ;assures that all the data received is identical.
            :test-fn      fetch-equiv-test}
           {:aspect       "correctly caches :stale-while-revalidate."
            :testing-args [fastest-cache-url 30]
            :should-be 1
            :test-fn      fetch-equiv-test}]})



(def service-worker-cache-conditional-tests
  {:on "Service Worker Cache-Conditional System"
   :tests [{:aspect "correctly does not cache :never-cache"
            :testing-args [(str rand-all-url-base "uncached/" (gensym "never"))
                           30]
            :should-be 30
            :test-fn fetch-equiv-test}
           {:aspect "correctly caches :cache-only"
            :testing-args [(str rand-all-url-base "allcached/" (gensym "always"))
                           30]
            :should-be 1
            :test-fn fetch-equiv-test}
           {:aspect "correctly races for :cache-fastest. Result will usually be 1 or 30, but as the cache updates the number will occasionally go somewhere in between."
            :non-deterministic true
            :testing-args [(str rand-all-url-base "fastest/" (gensym "faster"))
                           30]
            :test-fn fetch-equiv-test}]})



(def all-tests
  [service-worker-cache-conditional-tests
   service-worker-cache-paths-test])