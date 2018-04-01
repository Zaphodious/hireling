(ns hireling.core
  (:require [clojure.core.async :as async]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]))

(defn promise->chan! [promise]
  (let [resolved-promise (.resolve js/Promise promise)
        return-chan (async/chan)]
    (-> promise
        (.then (fn [a]
                 (async/go
                   (async/>! return-chan a)))
               (fn [e]
                 (async/go
                   (async/>! return-chan {::rejection e})))))
    return-chan))

(defn chan->promise! [chano]
  (js/Promise. (fn [resolve, reject]
                 (async/go
                   (let [chan-response (async/<! chano)]
                     (if (::rejection chan-response)
                       (reject (::rejection chan-response))
                       (resolve chan-response)))))))

(defn headers->map [headers-object]
  (walk/keywordize-keys (into {} (map vec (es6-iterator-seq (.entries headers-object))))))

(defn map->headers [headers-map]
  (js/Headers. (clj->js headers-map)))

(defn request->map [request-object]
  (let [req-bak (.clone request-object)]
    {:method          (.-method request-object)
     :url             (.-url request-object)
     :headers         (headers->map (.-headers request-object))
     :context         (.-context request-object)
     :referrer        (.-referrer request-object)
     :referrer-policy (.-referrerPolicy request-object)
     :mode            (.-mode request-object)
     :credentials     (.-credentials request-object)
     :redirect        (.-redirect request-object)
     :integrity       (.-integrity request-object)
     :cache           (.-cache request-object)
     :body            (.-body request-object)
     :body-used       (.-bodyUsed request-object)}))

(defn map->request [{:keys [url headers] :as request-map :or {headers {}, url "/"}}]
  (js/Request. url (clj->js (-> request-map
                                (dissoc :url)
                                (assoc :headers (map->headers headers))))))

(defn response->map! [response-object]
  (let [resp-bak (.clone response-object)]
    {:url           (.-url response-object)
     :headers       (headers->map (.-headers response-object))
     :status        (.-status response-object)
     :ok            (.-ok response-object)
     :status-text   (.-statusText response-object)
     :referrer      (.-referrer response-object)
     :type          (.-type response-object)
     :redirected    (.-redirected response-object)
     :use-final-url (.-useFinalURL response-object)
     :body          (.-body response-object)
     :body-used     (.-bodyUsed response-object)}))

(defn map->response! [{:keys [body headers] :as response-map :or {headers {}, body ""}}]
  (println "the headers are " headers)
  (js/Response. body (-> response-map
                         (dissoc :body)
                         (assoc :headers (map->headers headers))
                         clj->js)))

;(def default-fetch-opts
;  {:type :string
;   :method :get
;   :url ""})
;
;(defn fetch
;  [arg-opts-map]
;  (let [{:keys [url type] :as opts-map} (into default-fetch-opts arg-opts-map)
;        scope (if js/window js/window js/WorkerGlobalScope)
;        fetch-chan (promise->chan! (js/fetch url))
;        return-chan (async/chan)]
;    (async/go
;      (let [response (async/<! fetch-chan)]
;        (case type
;          :response (async/>! return-chan (response->map! response))
;          :string (async/take (promise->chan! (.text response))
;                              (fn [a] (async/>! return-chan a))))))
;    return-chan))

(defn which-cache-strategy? [path cached-paths]
  (let [cleaned-path (str/replace path (oget js/self :location :origin) "")]
    (->> cached-paths
         (filter (fn [[k vs]]
                   (vs cleaned-path)))
         (first)
         (first))))

(defn cache-the-response [resp-promise event]
  (.then resp-promise
         (fn [resp]
           (let [clone-resp (.clone resp)]
             (.then (.open js/caches (oget event :versionedCacheName))
                    (fn [cache]
                      (.put cache (oget event :request) clone-resp))))
           resp)))

(defn promise-from-cache [event]
  (-> (.match js/caches (oget event :request))))

(defn promise-from-network [event]
  (js/fetch (oget event :request)))

(defn network-with-cache [event]
  (let [initial-promise (promise-from-network event)
        return-chan (async/chan)]))


(defn handle-cache-only [event]
  (-> (promise-from-cache event)
      (.then (fn [resp]
               (if resp resp
                        (promise-from-network event))))))

(defn race-these
  "Returns a chan with the first result put on to these chans."
  [chan1 chan2]
  (->> (async/merge [chan1 chan2])
       (async/take 1)))

(defn handle-cache-fastest [event]
  (let [network-promise (cache-the-response (js/fetch (.-request event)) event)
        cache-promise (promise-from-cache event)]
    (chan->promise! (race-these (promise->chan! cache-promise) (promise->chan! network-promise)))))



(defmulti promise-for-strat (fn [strat-key event] strat-key))
(defmethod promise-for-strat nil [_ event] (promise-from-network event))
(defmethod promise-for-strat :cache-only [_ event] (handle-cache-only event))
(defmethod promise-for-strat :cache-fastest [_ event] (handle-cache-fastest event))
(defmethod promise-for-strat :cache-never [_ event] (promise-from-network event))

(defn default-on-fetch-handler [{:keys [event done-fn cached-paths cache-name version] :as event-params}]
  (oset! event :!versionedCacheName (str cache-name "_" version))
  (let [{:keys [cache-never cache-fastest cache-only]} cached-paths
        cache-strat (which-cache-strategy? (.-url (.-request event)) cached-paths)]
    (when cache-strat (println "provided cache strat is" cache-strat))
    (-> event (.respondWith
                (promise-for-strat cache-strat event)))))

(defn register-worker [worker-file-path]
  (when (.-serviceWorker js/navigator)
    (.. js/navigator -serviceWorker (register worker-file-path))))

(def default-worker {:version      1
                     :cache-name   "hireling-cache"
                     :cached-paths {:cache-never   [""]
                                    :cache-fastest [""]
                                    :cache-only    [""]}
                     :on-install!  (fn [{:keys [event done-fn version cache-name cached-paths]}]
                                     (.skipWaiting js/self)
                                     (println "host is " (.-host (.-location js/self)))
                                     (let [{:keys [cache-never cache-fastest cache-only]} cached-paths]
                                       (-> js/caches
                                           (.open (str cache-name "_" version))
                                           (.then (fn [cache]
                                                    (println "attempting to cache "
                                                             (clj->js (into cache-only cache-fastest)))
                                                    (.then (.addAll cache (clj->js (into cache-only cache-fastest)))
                                                           (fn [a] (done-fn))
                                                           (fn [a] (done-fn {::rejection "Failed to add everything."}))))))
                                       (println "No install declared for this service worker.")))
                     :on-activate! (fn [{:keys [event done-fn]}]
                                     (println "No activate declared for this worker.")
                                     (done-fn))
                     :on-fetch!    default-on-fetch-handler})

(defn path-vecs-to-sets [{:keys [cached-paths] :as combined-impl-map}]
  (assoc combined-impl-map :cached-paths (into {} (map (fn [[k v]]
                                                         {k (set v)})
                                                       cached-paths))))

(defn start-service-worker!
  "Starts the service worker with the provided options map."
  [provided-impl-map]
  (enable-console-print!)
  (let [[[install-done-chan install-done-fn]
         [activate-done-chan activate-done-fn]
         [fetch-done-chan fetch-done-fn]]
        (map
          (fn [ch] [ch (fn
                         ([] (async/put! ch :done))
                         ([a] (async/put! ch a))
                         ([a & bs] (async/put! ch (into [a] bs))))])
          (take 3 (repeatedly #(async/chan))))
        {:keys [on-install! on-activate! on-fetch!] :as combined-impl-map} (->> provided-impl-map
                                                                                (into default-worker)
                                                                                (path-vecs-to-sets))]
    (.addEventListener
      js/self "install"
      (fn [ev]
        (async/go (on-install! (into combined-impl-map {:event ev :done-fn install-done-fn})))
        (.waitUntil ev (chan->promise! install-done-chan))))
    (.addEventListener
      js/self "activate"
      (fn [ev]
        (async/go (on-activate! (into combined-impl-map {:event ev :done-fn activate-done-fn})))
        (.waitUntil ev (chan->promise! activate-done-chan))))
    (.addEventListener
      js/self "fetch"
      (fn [ev]
        (on-fetch! (into combined-impl-map {:event ev :done-fn fetch-done-fn}))))))
