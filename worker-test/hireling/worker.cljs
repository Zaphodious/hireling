(ns hireling.worker
  (:require [hireling.core :as hc]))

(enable-console-print!)

(println "Service Worker installs my friend!")

(hc/start-service-worker! {})