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
                       :cached-paths [""]})

(defn make-hireling
  "Creates a map that can be used by hireling's Service Worker lifecycle handler."
  [provided-impl-map]
  (into default-hireling provided-impl-map))

(defn open-cache [cache-name]
  (promise->chan! (.open js/caches cache-name)))

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

(defn map->request [{:keys [url headers] :as request-map}]
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

(defn map->response! [{:keys [body headers] :as response-map}]
  (js/Response. body (-> response-map
                         (dissoc :body)
                         (assoc :headers (map->headers headers)))))