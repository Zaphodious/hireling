(ns hireling.worker
  (:require [hireling.core :as hc]
            [bidi.bidi :as bidi]
            [hireling.routes :as hroutes]
            [clojure.string :as str]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]))

(enable-console-print!)

(defn clean-testing-route [patho] (str/replace patho (oget js/self :location :origin) ""))

(hc/start-service-worker! {; As service workers are updated in the browser when the file itself is byte-different
                           ; to a previous version, (and if you're including shared code with your sw build, it will
                           ; be byte-different fairly often,)  the :version number is used to ensure that certain
                           ; aspects of  the worker are as up-to-date as possible, and that some things aren't
                           ; needlessly discarded. If, for whatever reason, a hireling-defined service
                           ; worker begins installing and finds that it has a lower version number then
                           ; one already present, it will abort installation (note 1). In a future update, if a
                           ; hireling-defined service worker finds that it has the same version number as the one
                           ; previously installed it will ensure that its :cached-paths are up-to-date and then
                           ; immediately take over using the same cache. Also in a future update, if a hireling-defined
                           ; service worker finds that it has a greater version number then the one already installed, it
                           ; will being using a new cache, wait for the old version to become unused, and
                           ; then gracefully upgrade. The version should be bumped for breaking changes,
                           ; and needn't be bumped for things like simply adding a path or two to the cache.

                           ; (note 1) If you serve content using a service like cloudfront, older versions
                           ; of your files might be served depending on how often *those* caches are updated.
                           :version           2


                           ; App-specific name for your cache. The :version number will be appended before cache
                           ; creation.
                           :cache-name        "hireling-test-cache"


                           ; The :cached-paths entry takes, under various options, vectors
                           ; of path strings that are cached when the Service Worker loads and
                           ; returned (depending on the option's intent) when the main app requests
                           ; them. Note that these names are likely to change.
                           :cached-paths      {; Paths under :cache-never are never cached. Offline
                                               ; availability is the responsibility of the main app. Suitable
                                               ; only for resources that are managed by the main app. It is the
                                               ; responsibility of the originating server to ensure that proper
                                               ; no-caching headers are included, as occasionally a browser might
                                               ; still impose its own cache.
                                               :cache-never   [(bidi/path-for hroutes/routemap ::hroutes/never-cache-txt)]

                                               ; Paths under :cache-fastest are cached, requests are returned from the
                                               ; faster of the cache or network, and the values are updated regularly,
                                               ; once per day or when a network request succeeds (even if it loses
                                               ; to the cache). Best option for the app shell, compiled javascript,
                                               ; css, images, and anything else that might change but should
                                               ; arrive quickly in the browser. Resilient against low or no
                                               ; connectivity.
                                               :cache-fastest [(bidi/path-for hroutes/routemap ::hroutes/fastest-cache-txt)
                                                               (bidi/path-for hroutes/routemap ::hroutes/index)
                                                               (bidi/path-for hroutes/routemap ::hroutes/style)]

                                               ; Paths under :cache-only are cached once and not updated, though a
                                               ; cache miss will get the resource from the network.
                                               ; Should only be used for expensive-to-get and never-changing resources
                                               ; like large files, as :cache-fastest provides insurance against disk
                                               ; access being slow *and* it frequently updates.
                                               :cache-first   [(bidi/path-for hroutes/routemap ::hroutes/always-cache-txt)]}


                           ; The :cache-conditional entry takes the same options as :cached-paths,
                           ; but instead of a vector of path strings each option maps to a function taking
                           ; the request path and returning a boolean. Any time a request is made
                           ; and the url is not found within :cached-paths, its checked against :cache-conditional.
                           :cache-conditional {:cache-never   (fn [patho]
                                                                (= ::hroutes/rand-all-uncached
                                                                   (:handler (bidi/match-route hroutes/routemap (clean-testing-route patho)))))
                                               :cache-fastest (fn [patho]
                                                                (#{::hroutes/main-js ::hroutes/rand-all-fastest-cached}
                                                                  (:handler (bidi/match-route hroutes/routemap (clean-testing-route patho)))))
                                               :cache-first   (fn [patho]
                                                                (= ::hroutes/rand-all-cached
                                                                   (:handler (bidi/match-route hroutes/routemap (clean-testing-route patho)))))}

                           ; The :send-later option will handle POST and PUT requests, and accepts a map of
                           ; {fn (urlstring)->boolean, [frequencey-of-request, max-requests] }. If the function evals
                           ; to true, the request is cached and repeatedly sent until the server indicates success.
                           ; The response returned to the main in-browser app is a path that if fetched returns
                           ; either ":sent" if the request has been sent, ":weaiting" if the request has not yet
                           ; been received, ":rejected" if the request was rejected by the server, or ":not-found"
                           ; if the request-id is not found in the system. The request is deleted from the handler
                           ; after max-requests has been exceeded. Note that because these requests could, potentially
                           ; be sent multiple times, they should be idempotent.
                           :send-later        {(constantly false) [0, 0]}

                           ; The :fallback option will take a urlstring for a fallback page that will be shown when the
                           ; user is reasonably thought to be offline and all other caching has failed. Last resort,
                           ; explicitly to avoid seeing the browser's "sorry, you're offline" page. to be stored separately
                           ; from other data.
                           :fallback          ""

                           ; The :cache-intercept entry will allow developers to fully take control of
                           ; response behavior without overriding everything. It will take a map of
                           ; {fn taking (request, callback-boolean), fn taking (request, callback-response)},
                           ; where request is the js http request provided by the service worker api, the callbacks
                           ; are functions where values put for return, and response is the js http response as
                           ; consumed by the service worker api. Callbacks are utilized as the worker API is highly
                           ; async, so regular returns are often impossible. Will be included as a safety valve in case
                           ; functionality is required that is not yet provided for by the default hireling
                           ; implementation or its extension api.
                           :cache-intercept   {(constantly false) nil}})
