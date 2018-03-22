(ns hireling.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [hireling.core-test]))

(doo-tests 'hireling.core-test)
