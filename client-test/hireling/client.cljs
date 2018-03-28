(ns hireling.client
  (:require [rum.core :as rum]
            [hireling.test-runner :as tester]
            [hireling.core :as hireling]))

(enable-console-print!)

(println "This text is printed from src/hirelingtest/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(hireling/register-worker "worker.js")

(rum/defc hello-world []
  (tester/testing-demo))

(rum/mount (hello-world)
           (. js/document (getElementById "app")))

(defn on-js-reload [])
;; optionally touch your app-state to force rerendering depending on
;; your application
;; (swap! app-state update-in [:__figwheel_counter] inc)

