(ns hireling.core)

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defn wrap-promise [{:keys [promise event-translator] :or {event-translator (fn [a] a)}}]
  (let [callback-success (fn [a] (event-translator a))]))
