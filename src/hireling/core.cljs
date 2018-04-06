(ns hireling.core
  (:require [clojure.core.async :as async]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]))

(set! *warn-on-infer* true)

(defn load-workbox
  "Loads the workbox library.

  Params are
      none: Loads from Google's CDN.
      workbox-uri: the url of workbox-sw.js,
      module-prefix: the directory where the script is located.
      See https://developers.google.com/web/tools/workbox/modules/workbox-sw#using_local_workbox_files_instead_of_cdn

  Returns the workbox object."
  ([] (load-workbox "https://storage.googleapis.com/workbox-cdn/releases/3.0.1/workbox-sw.js" nil))
  ([workbox-uri module-prefix] (js/importScripts workbox-uri)
   (let [mod-prefix-opts ^js/object (clj->js {"modulePathPrefix" module-prefix})]
     (when module-prefix (.setConfig js/workbox mod-prefix-opts)))
   js/workbox))

(defn register-service-worker [worker-file-path]
  (when (and (.-serviceWorker js/navigator) js/caches)
    (.. js/navigator -serviceWorker (register worker-file-path))))

(def default-worker {:version         1
                     :cache-name      "hireling-cache"
                     :precached-paths {:cache-never [""]
                                       :cache-fastest  [""]
                                       :cache-first    [""]}
                     :cache-conditional {:cache-never (constantly false)
                                         :cache-fastest (constantly false)
                                         :cache-first (constantly false)}})

(defn path-vecs-to-sets [{:keys [precached-paths] :as combined-impl-map}]
  (assoc combined-impl-map :precached-paths (into {} (map (fn [[k v]]
                                                           {k (set v)})
                                                          precached-paths))))

(defn clients-claim [] (.clientsClaim js/workbox))
(defn skipWaiting [] (.skipWaiting js/workbox))

(defn set-cache-name-details! [{:keys [prefix suffix precache runtime googleAnalytics] :as details-map}]
  (-> js/workbox .-core (.setCacheNameDetails (clj->js details-map))))

(defn precache-and-route!
  "Takes a vector of strings, and a map of options.
   Precaches the paths and returns them upon subsequent visits"
  [path-vec {:keys [directoryindex ignoreUrlParametersMatching cleanUrls urlManipulation] :as options-map}]
  (-> js/workbox .-precaching (.precacheAndRoute (clj->js path-vec)
                                                 (clj->js options-map))))

(defn register-navigation-route! [{:keys [url cache-name blacklist whitelist]}]
  (let [js-blacklist (if blacklist (clj->js blacklist) #js[])
        js-whitelist (if whitelist (clj->js whitelist) #js[])
        opts (into {:blacklist js-blacklist
                    :whitelist js-whitelist}
               (when cache-name {:cacheName cache-name}))]
    (println "nav route is " (-> js/workbox .-routing (.registerNavigationRoute url (clj->js opts))))))

(defn handler-fn-for [handler-key {:keys [cache-name max-entries max-age-seconds] :as strat-map}]
  (let [strat (.-strategies js/workbox)
        s-opts #js{:cacheName cache-name :cacheExpiration #js{:maxEntries max-entries :maxAgeSeconds max-age-seconds}}]
    (case handler-key
      :stale-while-revalidate (.staleWhileRevalidate strat s-opts)
      :cache-first (.cacheFirst strat s-opts)
      :cache-only (.cacheOnly strat s-opts)
      :network-first (.networkOnly strat s-opts)
      :network-only (.networkOnly strat s-opts))))

(defn make-cache-name [app-name cache-name version-number]
  (str app-name "-" cache-name "-v" version-number))

(defn register-route! [{:keys [route strategy strategy-options method cache-name app-name max-entries max-age-seconds version]}]
  (let [updated-strat-opts {:cache-name (make-cache-name app-name cache-name version)
                            :max-entries max-entries
                            :max-age-seconds max-age-seconds}
        handler-fn (handler-fn-for strategy updated-strat-opts)
        made-cache-name (make-cache-name app-name cache-name version)]
    (-> js/workbox .-routing (.registerRoute route handler-fn (str/upper-case (if method (name method) "GET"))))))

(defn- update-cache-name-as [cache-name]
  (fn [a] (update-in a [:strategy-options :cache-name] (fn [b] (if b b cache-name)))))


(defn start-service-worker! [{:keys [workbox-uri workbox-uri-prefix cache-routes cache-name version
                                     app-name precaching precache-routing-opts navigation-route] :as provided-impl-map}]
  (enable-console-print!)
  (if workbox-uri (load-workbox workbox-uri workbox-uri-prefix) (load-workbox))
  (let [cache-entries cache-routes]
    (println "\uD83C\uDF88\uD83C\uDF88\uD83C\uDF88 These are " cache-entries)
    (set-cache-name-details! {:prefix app-name :suffix (str "v" version)
                              :precache "precache" :runtime "runtimecache"})
    (println "is this an array? " (.isArray js/Array (clj->js [:hello])))
    (println "precache entries are " (to-array precaching))
    (println "precaching result is "
             (when precaching (-> js/workbox (.-precaching) (.precache (clj->js precaching)))))
    (when precache-routing-opts (-> js/workbox .-precaching .addRoute (clj->js precache-routing-opts)))
    (when navigation-route (register-navigation-route! navigation-route))
    (doall
      (->> cache-entries
        (map (fn [a] (assoc a :app-name app-name)))
        (map (fn [a] (assoc a :version version)))
        (map register-route!)))))


