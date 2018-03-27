(ns hireling.gardener
  (:require [garden.core :as garden]
            [garden.color :as color]))

(def styledec [:* {:background-color (color/rgb 240 240 240)}])

(defn compile-style
  []
  (garden/css styledec))