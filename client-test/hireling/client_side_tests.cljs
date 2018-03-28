(ns hireling.client-side-tests
  (:require [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-prot]
            [hireling.core :as hc]
            [clojure.walk :as walk]))


(def promise-tests
  {:on    "The Promise-Chan Converter Set"
   :tests [{:aspect  "Returns a channel when given a promise"
            :test-fn #(% true)}]})