(ns hireling.routes
  (:require [bidi.bidi :as bidi]))



(def routemap ["/" {"js/" {true ::main-js}
                    "worker.js" ::worker-js
                    "out/" ::worker-js-assets
                    #{"" "index.html"} ::index
                    "style.css" ::style
                    "simple.txt" ::simple-txt
                    "rand/" {"always-cached.txt" ::always-cache-txt
                             "never-cached.txt" ::never-cache-txt
                             "cache-updates.txt" ::fastest-cache-txt
                             "all/" {true ::rand-all}}}])