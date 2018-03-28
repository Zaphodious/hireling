(ns hireling.test-runner
  (:require [rum.core :as rum]
            [clojure.core.async :as async]))

(defn start-test [{:keys [testing aspect should-be test-fn testing-args async?] :as test-def-map :or {[] testing-args}}]
  (let [async-atom (atom ::async-waiting)
        async-testing-args (if async?
                             (into [(fn [a] (reset! async-atom a))] testing-args)
                             testing-args)
        {:keys [actually error]
         :as   invocation-result}
        (try
          {:actually (apply test-fn async-testing-args)}
          (catch :default e
            {:actually :error
             :error    e}))
        pass? (= should-be actually)]
    (-> test-def-map
        (into invocation-result)
        (assoc :async-atom async-atom))))

(defn get-test-status [{:keys [testing aspect should-be test-fn testing-args actually error async-atom async?] :as test-def-map :or {[] testing-arg-vec}}]
  (let [actual-actually (if async?
                          @async-atom
                          actually)
        passing? (= actual-actually should-be)
        waiting? (and async? (= actual-actually ::async-waiting))]
    (if passing? :passing (if (and async? waiting?) :waiting :failing))))

(rum/defc render-test-result < rum/reactive
  [{:keys [testing aspect should-be test-fn testing-args test-result async?] :as test-def-map :or {[] testing-arg-vec}}]
  (let [{:keys [actually error async-atom]
         :as   invocation-result}
        (if test-result test-result
                        (:test-result (start-test test-def-map)))
        async-result (rum/react async-atom)
        {:keys [passing? waiting?]} (get-test-status test-def-map)]
    (println "async atom is " async-atom)
    (println "async result is " async-result)
    (println "are we waiting? " waiting?)
    [:.test-result {:class (if waiting? "waiting" (if passing? "success" "failure"))
                    :key   (str aspect "-test")}
     [(when testing [:.tested-thing testing])
      [:.aspect aspect]
      (when-not (or passing? waiting?)
        [:.result [[:.message (str "Expecting " (pr-str should-be) ", but got " (pr-str actually))]
                   (when error [:.error (.-stack error)])]])]]))

(rum/defc render-test < rum/reactive
  [{:keys [testing aspect should-be test-fn testing-args actually error async-atom async?] :as test-def-map}]
  (let [status (get-test-status test-def-map)]
    [:.test-result {:class (name status)}]))

(rum/defc tests-on < rum/static
  [{:keys [on tests]}]
  (let [results-pre-insert (map start-test tests)
        results (map #(assoc % :testing on) results-pre-insert)
        renders (map render-test results)
        test-count (count results)
        fail-count (count (filter #(= :failing (get-test-status %))
                                  results))]
    [:.test-collection
     {:class (if (= 0 fail-count) "passing" "failing")}
     [:h3 "Testing " on]
     [:.stats "Failing: " fail-count " out of " test-count]
     (into [] renders)]))

(defn testing-demo []
  (tests-on {:on    "The Error System"
             :tests [{:aspect       "shows errors"
                      :should-be    0
                      :test-fn      #(throw (js/Error. "Seeing what the error looks like."))
                      :testing-args [[2]]}
                     {:aspect       "shows success"
                      :should-be    2
                      :test-fn      #(+ 1 %)
                      :testing-args [1]}
                     {:aspect       "shows another success"
                      :should-be    "aa"
                      :test-fn      #(str "a" %)
                      :testing-args ["a"]}
                     {:aspect       "shows failure"
                      :should-be    1
                      :test-fn      #(+ 1 %)
                      :testing-args [2]}
                     {:aspect    "runs async tests"
                      :should-be 100000
                      :async? true
                      :test-fn   (fn [f]
                                   (async/go
                                     (f (count (take 10000 (repeat :a))))))}]}))