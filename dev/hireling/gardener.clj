(ns hireling.gardener
  (:require [garden.core :as garden]
            [garden.color :as color]))

(defn make-seq-background-color [the-color]
  (assoc (color/lighten (color/as-rgb the-color) 20) :alpha 0.8))

(defn make-test-background-color [the-color]
  (assoc (color/lighten (color/as-rgb the-color) 40) :alpha 0.8))

(defn make-seq-shadow [the-color]
  (str "inset 0px 0px 19px -1px " (color/as-hex (color/darken the-color 15))))

(defn make-test-shadow [the-color]
  (str "0px 0px 24px 0px " (color/as-hex (color/lighten the-color 15))))

(def pass-color (color/from-name :green))
(def fail-color (color/from-name :red))
(def wait-color (color/from-name :purple))
(def test-coll-color-pass (color/from-name :forestgreen))
(def test-coll-color-fail (color/from-name :firebrick))

(def styledec [:html {:background-color :black}
               [:* {:font-family "sans-serif"}]
               [:.test-collection {:border-color :black
                                   :border-width :1px
                                   :border-style :solid}
                [:&.passing {:background-color (make-seq-background-color test-coll-color-pass)
                             :box-shadow (make-seq-shadow test-coll-color-pass)}]
                [:&.failing {:background-color (make-seq-background-color test-coll-color-fail)
                             :box-shadow (make-seq-shadow test-coll-color-fail)}]
                [:h3 {:padding 0
                      :margin :7px
                      :font-size :2em}]
                [:.stats {:margin :6px
                          :margin-left :30px
                          :padding 0}]]
               [:.test-result {:padding      :5px
                                :margin       :10px
                                :overflow-x   :wrap
                                :border-width :1px
                                :border-style :solid
                                ;:border-radius :10px
                                :box-shadow   ""}
                 [:.aspect [:&:before {:content "\" \""}]]
                 [:.tested-thing :.aspect {:font-size :1.5em
                                           :display   :inline}]
                 [:&.passing {:border-color     :green
                              :box-shadow       (make-test-shadow pass-color)
                              :background-color (make-test-background-color pass-color)}
                  [:.tested-thing [:&:before {:content "\"Pass: \""}]]]
                 [:&.waiting {:border-color     :purple
                              :box-shadow       (make-test-shadow wait-color)
                              :background-color (make-test-background-color wait-color)}
                  [:.tested-thing [:&:before {:content "\"Waiting: \""}]]]
                 [:&.failing {:border-color     :red
                              :box-shadow       (make-test-shadow fail-color)
                              :background-color (make-test-background-color fail-color)}
                  [:.tested-thing [:&:before {:content "\"Fail: \""}]]]]])



(defn compile-style
  []
  (garden/css styledec))