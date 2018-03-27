(ns hireling.test-runner
  (:require [rum.core :as rum]))

(defn execute-test [{:keys [testing aspect should-be test-fn testing-args] :as test-def-map :or {[] testing-arg-vec}}]
  (let [{:keys [actually error]
         :as   invocation-result}
        (try
          {:actually (apply test-fn testing-args)}
          (catch :default e
            {:actually :error
             :error    e}))
        pass? (= should-be actually)]
    (->> (assoc invocation-result :pass? pass?)
         (assoc test-def-map :test-result))))


(rum/defc testing < rum/static
  [{:keys [testing aspect should-be test-fn testing-args test-result] :as test-def-map :or {[] testing-arg-vec}}]
  (let [{:keys [actually error pass?]
         :as   invocation-result}
        (if test-result test-result
                        (:test-result (execute-test test-def-map)))]
    [:.test-result {:class (if pass? "success" "failure")
                    :key   (str aspect "-test")}
     [(when testing [:.tested-thing testing])
      [:.aspect aspect]
      (when-not pass?
        [:.result [[:.message (str "Expecting " (pr-str should-be) ", but got " (pr-str actually))]
                   (when error [:.error (.-stack error)])]])]]))

(rum/defc tests-on < rum/static
  [{:keys [on tests]}]
  (let [results-pre-insert (map execute-test tests)
        results (map #(assoc % :testing on) results-pre-insert)
        renders (map testing results)
        test-count (count results)
        fail-count (count (filter #(not (:pass? (:test-result %)))
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
                      :testing-args [2]}]}))