(ns hireling.worker
  (:require [hireling.core :as hc]
            [bidi.bidi :as bidi]
            [hireling.routes :as hroutes]
            [clojure.string :as str]))

(enable-console-print!)


(defn clean-testing-route [patho] (str/replace (.toString (.-url patho)) ^js/string (.toString (-> js/self .-location .-origin))
                                               ""))

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

                           ; Name of the app. Used to create the cache name, along with the version number.
                           :app-name "hireling-test"
                           ; A vec of routes to be precached. Note that if any of these URLs fail to cache, the service
                           ; worker itself will fail to
                           :precaching [{; URL that should be precached. Naturally, in order to
                                         ; precache an asset, the url must be known ahead of time.
                                         ; Thus, no functions or regex. Only strings.
                                         :url "/index.html"
                                         ; Version of the URL to cache. Should be incremented when the asset is changed.
                                         ; Without a revision, the asset can never be updated.
                                         :revision 1
                                         ; If :add-route? is true, a handler will be registered that returns the pre-cached
                                         ; asset. If false, another handler must be declared elsewhere.
                                         :add-route? false}
                                        {:url "/rand/precached.txt"
                                         :revision 1
                                         :add-route? true}]
                           :precache-routing-opts {:directoryIndex "index.html"}

                           ; Takes a vector of maps that will be used to determine the service worker's caching behavior.
                           :cache-routes [{; Strategy the route will use to determine caches. Options
                                           ; are #{:cache-first :cache-only :network-first :network-only :stale-while-revalidate},
                                           ; which correspond to the options
                                           ; in https://developers.google.com/web/tools/workbox/reference-docs/latest/workbox.strategies
                                           ; Note - :cache-only is somewhat dangerous, as without special handlers
                                           ; the user will not be able to get the asset *at all*.
                                           :strategy :stale-while-revalidate

                                           ; (Optional) Next three are options that modify the strategy. See
                                           ; https://developers.google.com/web/tools/workbox/reference-docs/latest/workbox.strategies
                                           ; Name of cache to use for caching (both lookup and updating).
                                           ; Will have the :app-name as the prefix.
                                           ; Will be  versioned with :version.
                                           :cache-name nil
                                           ; The maximum number of entries to store in a cache.
                                           :max-entries 1
                                           ; The maximum lifetime of a request to stay in the cache before it's removed.
                                           :max-age-seconds (* 60 60 2)
                                           ; Route is a string, regex, or predicate function used to match a request
                                           ; to its cache entry and caching strategy.
                                           :route (bidi/path-for hroutes/routemap ::hroutes/fastest-cache-txt)
                                           ; Method is the HTTP method that will be listened for. Defaults to :GET,
                                           ; additional options are #{:PUT :POST :DELETE :HEAD}
                                           :method :GET}
                                          {:strategy :cache-first
                                           :route (bidi/path-for hroutes/routemap ::hroutes/always-cache-txt)}
                                          {:strategy :cache-first
                                           :route (fn [patho]
                                                    (println "patho is " patho)
                                                    (= ::hroutes/rand-all-cached
                                                       (:handler (bidi/match-route hroutes/routemap (clean-testing-route patho)))))}
                                          {:strategy :cache-first
                                           :route #"rand/all/regexd"}
                                          {:strategy :stale-while-revalidate
                                           :route "/"}
                                          {:strategy :stale-while-revalidate
                                           :cache-name "jscache"
                                           :route #".js"}
                                          {:strategy :stale-while-revalidate
                                           :cache-name "csscache"
                                           :route #".css"}]

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
