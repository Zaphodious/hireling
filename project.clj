(defproject hireling "0.5.0-ALPHA-SNAPSHOT"
  :description "A clojurescript library for working with service workers."
  :url "https://github.com/Zaphodious/hireling"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [camel-snake-kebab "0.4.0"]]

  :source-paths ["src"]
  :test-paths ["client-test" "worker-test" "dev"]

  :profiles
  {:dev {:dependencies [[http-kit "2.2.0"]
                        [org.clojure/tools.nrepl "0.2.12"]
                        [ring/ring-core "1.6.3"]
                        [rum "0.11.2"]
                        [bidi "2.1.3"]
                        [garden "1.3.4"]
                        [org.clojure/tools.logging "0.4.0"]
                        [ring-cljsbuild "2.1.3"]]
         :source-paths ["dev" "worker-test" "src" "client-test"]}})
