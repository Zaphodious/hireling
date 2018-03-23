(ns hireling.core
  (:require [clojure.core.async :as async]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defn wrap-promise! [{:keys [promise event-translator] :or {event-translator (fn [a] a)}}]
  (let [resolved-promise (.resolve js/Promise promise)
        return-chan (async/chan)]
    (-> promise
        (.then (fn [a]
                (println "resolved promise as " a)
                (async/go
                  (async/>! return-chan (event-translator {:type :success :value a}))))
               (fn [e]
                 (println "promise rejects to " e)
                 (async/go
                   (async/>! return-chan {:type :rejection :reason e})))))
    return-chan))
