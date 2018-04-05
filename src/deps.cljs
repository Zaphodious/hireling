{:externs ["workbox/workbox-externs.js"]
 :language-in  :es6
 :rewrite-polyfills true}
#_(into
    [{:file "workbox/workbox-sw.js"
      :provides ["workbox"]}]
    (map
      (fn [a]
        {:file (str "workbox/workbox-"a".dev.js")
         :file-min (str "workbox/workbox-"a".prod.js")
         :provides [(str "workbox."a)]})
      ["core" "background-sync" "broadcast-cache-date"
       "cache-expiration" "cacheable-response"
       "google-analytics" "precaching" "range-requests"
       "routing" "strategies"]))
(println "deps file eval'd")