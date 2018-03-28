(ns hireling.test-runner
  (:require [rum.core :as rum]
            [clojure.core.async :as async]))

(defn start-test [{:keys [testing aspect should-be test-fn testing-args] :as test-def-map :or {[] testing-args}}]
  (let [async-atom (atom ::async-waiting)
        test-over-fn (fn [a] (reset! async-atom a))
        async-testing-args (into [test-over-fn] testing-args)]
    (async/go
      (try
        (apply test-fn async-testing-args)
        (catch :default e
          (test-over-fn {::error e}))))
    (-> test-def-map
        (assoc :async-atom async-atom))))

(defn get-test-status [{:keys [testing aspect should-be test-fn testing-args async-atom] :as test-def-map :or {[] testing-arg-vec}}]
  (let [actually @async-atom
        error? (::error actually)
        passing? (= actually should-be)
        waiting? (= actually ::async-waiting)]
    (cond
      error? :failing
      passing? :passing
      waiting? :waiting
      :else :failing)))

(rum/defc render-test < rum/reactive
  [{:keys [testing aspect should-be test-fn testing-args actually async-atom] :as test-def-map}]
  (let [result (rum/react async-atom)
        status (get-test-status test-def-map)
        error (::error result)]
    [:.test-result {:class (name status)
                    :key (str aspect "-test")}
     [(when testing [:.tested-thing testing])
      [:.aspect aspect]
      (when-not (or (= status :passing) (= status :waiting))
        [:.result [[:.message (str "Expecting " (pr-str should-be) ", but got " (if error
                                                                                  (js->clj error)
                                                                                  (pr-str result)))]
                   (when error
                     (do
                       (println (.-stack error))
                       (.-stack error)))]])]]))

(rum/defc render-test-category < rum/reactive
  [{:keys [on tests results]}]
  (let [renders (map render-test results)
        test-count (count results)
        ensure-reactive (doall (map (fn [a] (rum/react (:async-atom a))) results))
        all-status (map get-test-status results)
        fail-count (count (filter #(= :failing %)
                                  all-status))
        waiting-count (count (filter #(= :waiting %)
                                     all-status))]
    [:.test-collection
     {:class (cond
               (< 0 waiting-count) "waiting"
               (< 0 fail-count) "failing"
               :else "passing")}
     [:h3 "Testing " on]
     [:.stats (str "Failing: " fail-count " out of " test-count " | Waiting: " waiting-count " out of " test-count)]
     (into [] renders)]))

(defn tests-on
  [{:keys [on tests] :as test-decs}]
  (let [results-pre-insert (map start-test tests)
        results (map #(assoc % :testing on) results-pre-insert)]
    (render-test-category (assoc test-decs :results results))))

(defn testing-demo []
  (tests-on {:on    "The Error System"
             :tests [{:aspect       "shows errors"
                      :should-be    0
                      :test-fn      #(%1 (throw (js/Error. "Seeing what the error looks like.")))
                      :testing-args [[2]]}
                     {:aspect       "shows success"
                      :should-be    2
                      :test-fn      #(%1 (+ 1 %2))
                      :testing-args [1]}
                     {:aspect       "shows another success"
                      :should-be    "aa"
                      :test-fn      #(%1 (str "a" %2))
                      :testing-args ["a"]}
                     {:aspect       "shows failure"
                      :should-be    1
                      :test-fn      #(%1 (+ 1 %2))
                      :testing-args [2]}
                     {:aspect    "runs async tests"
                      :should-be 100000
                      :test-fn   (fn [f]
                                   (async/go
                                     (f (count (take 100000 (repeat :a))))))}]}))