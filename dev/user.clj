(ns user
  (:require [rum.core :as rum]
            [bidi.bidi :as bidi]
            [bidi.ring :as bring]
            [hireling.gardener :as gardener]
            [hireling.routes :as hroutes]
            [ring.util.response :as resp]
            [ring-cljsbuild.core :as ring-cljs]
            [org.httpkit.server :as server :refer [run-server]]))

(rum/defc home-page []
  [:html
   [:!DOCTYPE {"html" "html"}]
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:href "style.css" :rel "stylesheet" :type "text/css"}]
    [:link {:href "style.css" :rel "preload" :as "style"}]
    [:link {:href "/js/main.js" :rel "preload" :as "script"}]]
   [:body
    [:div {:id "app"}
     [:h2 "Pre-JS Template"]]
    [:script {:src "/js/main.js" :type "text/javascript"}]]])

(defn index-handler [request]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (rum/render-static-markup (home-page))})

(defn style-handler [request]
  {:status  200
   :headers {"Content-type" "text/css"}
   :body    (gardener/compile-style)})

(defn rand-handler [request]
  {:status  200
   :headers {"Content-type" "text/plain"}
   :body    (pr-str (take 5 (repeatedly #(rand-int 999))))}) ;Probably won't return the same sequence. Probably.

(defn main-js-handler [request]
  {:status  200
   :headers {"Content-type" "text/css"}
   :body    "?"})

(defn simple-txt-handler [request]
  {:status  200
   :headers {"Content-type" "text/plain"}
   :body    "I'm from a server!"})

(defn main-js-builder [handler-fn]
  (ring-cljs/wrap-cljsbuild
    handler-fn
    "/js/"
    {:id           :main-js
     :auto         true
     :java-logging false
     :main-js-name "main.js"
     :source-map   true
     :cljsbuild    {:source-paths ["src" "client-test"]
                    :incremental  true
                    :compiler     {:optimizations  :none
                                   ;:optimizations  :advanced
                                   :infer-externs true
                                   :cache-analysis true
                                   :pretty-print   false
                                   :warnings       true
                                   :main           "hireling.client"}}}))

(defn worker-js-builder [handler-fn]
  (ring-cljs/wrap-cljsbuild
    handler-fn
    ""
    {:id           :worker-js
     :auto         true
     :java-logging false
     :main-js-name "worker.js"
     :source-map   true
     :cljsbuild    {:source-paths ["src" "worker-test"]
                    :incremental  true
                    :compiler     {;:optimizations  :none
                                   :optimizations  :advanced
                                   :infer-externs true
                                   :cache-analysis true
                                   :pretty-print   false
                                   :warnings       true
                                   :target         :webworker
                                   :main           "hireling.worker"}}}))

(declare route-matcher handler)

(def handler
  (bring/make-handler hroutes/routemap
                      (fn [route-key]
                        (case route-key
                          ::hroutes/index index-handler
                          ::hroutes/main-js (main-js-builder handler)
                          ::hroutes/worker-js (worker-js-builder handler)
                          ::hroutes/worker-js-assets (worker-js-builder handler)
                          ::hroutes/style style-handler
                          ::hroutes/simple-txt simple-txt-handler
                          ::hroutes/rand-all rand-handler
                          ::hroutes/rand-all-uncached rand-handler
                          ::hroutes/rand-all-cached rand-handler
                          ::hroutes/rand-all-fastest-cached rand-handler
                          ::hroutes/always-cache-txt rand-handler
                          ::hroutes/never-cache-txt rand-handler
                          ::hroutes/fastest-cache-txt rand-handler
                          ::hroutes/rand-regex rand-handler))))


(defonce
  stop-fn-atom (atom
                 (fn []
                   (println "no server to stop!"))))


(defn start []
  (let [server-stop-fn (run-server
                         #'handler
                         {:port 3000})
        clear-stop-fn (fn []
                        (do
                          (server-stop-fn)
                          (reset! stop-fn-atom (fn [] (println "Nothing to see here!")))))]
    (reset! stop-fn-atom clear-stop-fn)))

(defn stop []
  (@stop-fn-atom))

(defn reset []
  (do
    (stop)
    (start)))

;(ocall workbox :precaching :precacheAndRoute cache-fastest (ocall workbox :strategies :staleWhileRevalidate)))
