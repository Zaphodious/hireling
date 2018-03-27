(defproject hireling "0.1.0-SNAPSHOT"
  :description "A clojurescript library for working with service workers."
  :url "https://github.com/Zaphodious/hireling"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.9"]]
  :doo {:paths {:rhino "lein run -m org.mozilla.javascript.tools.shell.Main"}}
  :aliases {"test-loop" ["with-profile" "test" "doo" "chrome" "test" "auto"]
            "test-once" ["with-profile" "test" "doo" "chrome" "test" "auto"]}
  :cljsbuild {:builds
              [{:id "dev"
                :compiler {:output-to "target/main.js"
                           :output-dir "target"
                           :main hireling.test-runner
                           :optimizations :simple}}]}

  :profiles
  {:test {:dependencies [[org.mozilla/rhino "1.7.9"]]
          :cljsbuild
          {:builds
            {:test
             {:source-paths ["src" "test"]
              :compiler {:output-to "target/main.js"
                         :output-dir "target"
                         :main hireling.test-runner
                         :optimizations :simple}}}}}})
