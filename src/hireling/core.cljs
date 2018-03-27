(ns hireling.core
  (:require [clojure.core.async :as async]
            [clojure.walk :as walk]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defn promise->chan! [promise]
  (let [resolved-promise (.resolve js/Promise promise)
        return-chan (async/chan)]
    (-> promise
        (.then (fn [a]
                (println "resolved promise as " a)
                (async/go
                  (async/>! return-chan a)))
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
                       :cached-paths [""]
                       :on-install! (fn [a] a)})

(defn start-service-worker!
  "Starts the service worker with the provided options map."
  [provided-impl-map]
  (enable-console-print!)
  (let [{:keys [on-install!] :as combined-impl-map} (into default-hireling provided-impl-map)
        [install-event-chan] (take 1 (repeatedly #(async/chan)))]
    (.addEventListener
      js/self "install"
      (fn [ev]
        (async/go (async/>! install-event-chan (on-install!)))
        (.waitUntil ev (chan->promise! install-event-chan))))))

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


(defn register-worker [worker-file-path]
  (when (.-serviceWorker js/navigator)
    (.. js/navigator -serviceWorker (register worker-file-path))))

