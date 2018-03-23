(ns hireling.core
  (:require [clojure.core.async :as async]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defn promise->chan! [{:keys [promise event-translator] :or {event-translator (fn [a] a)}}]
  (let [resolved-promise (.resolve js/Promise promise)
        return-chan (async/chan)]
    (-> promise
        (.then (fn [a]
                (println "resolved promise as " a)
                (async/go
                  (async/>! return-chan (event-translator a))))
               (fn [e]
                 (println "promise rejects to " e)
                 (async/go
                   (async/>! return-chan {:type :rejection :reason e})))))
    return-chan))

(defn chan->promise! [chano]
  (js/Promise. (fn [resolve, reject]
                 (async/go
                   (let [chan-response (async/<! chano)]
                     (resolve chan-response))))))

(def default-hireling {:version 1
                       :cache-name "hireling-cache"
                       :cached-paths [""]})

(defn make-hireling
  "Creates a map that can be used by hireling's Service Worker lifecycle handler."
  [provided-impl-map]
  (into default-hireling provided-impl-map))
