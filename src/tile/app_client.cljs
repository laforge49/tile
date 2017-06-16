(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]
            [tile.core :as tile]))

(defn make-b1
  []
  (tile/basic-tile-state-atom
    (fn [state]
      [:div
       [:input {:type "button"
                :value "chsk-send! (with reply)"
                :on-click (fn []
                            (->output!
                              "Button 2 was clicked (will receive reply from server)")
                            (chsk-send!
                              [:example/button2 {:had-a-callback? "indeed"}]
                              5000
                              (fn [cb-reply]
                                (->output! "Callback reply: %s" cb-reply))))}]])))

(defn make-b2
  []
  (tile/basic-tile-state-atom
    (fn [state]
      [:div 222])))

(defn make-l2
  []
  (tile/list-tile-state-atom))

(defn make-l1
  []
  (tile/list-tile-state-atom))

(defn make-m1
  []
  (tile/map-tile-state-atom
    {}
    {:a 1 :b 2 :c 3}))

(defn make-m2
  []
  (tile/map-tile-state-atom
    {}
    {:a 1 :b {:x 55 :y :apples} :c 3}))

(defn calling-component
  []
    #_(tile/add-child-tile l1 m1)
    #_(tile/add-child-tile l1 m2)
    #_(tile/add-child-tile l1 l2)
    #_(tile/add-child-tile l1 b1)
    #_(tile/add-child-tile l2 b2)
  (let [l1
        (tile/create-tile-state
          make-l1
          "Basic tile example")
        b1
        (tile/create-tile-state
          make-b1
          "Test")
        b2
        (tile/create-tile-state
          make-b2
          "Test2")
        m1
        (tile/create-tile-state
          make-m1
          "Basic map")
        m2
        (tile/create-tile-state
          make-m2
          "Map2")
        l2
        (tile/create-tile-state
          make-l2
          "Sub-list")]
    (swap! b1 assoc :parent-tile-ndx (:tile-ndx @l1))
    (swap! m1 assoc :parent-tile-ndx (:tile-ndx @l1))
    (swap! m2 assoc :parent-tile-ndx (:tile-ndx @l1))
    (swap! l2 assoc :parent-tile-ndx (:tile-ndx @l1))
    (swap! b2 assoc :parent-tile-ndx (:tile-ndx @l2)))
  (fn []
      [tile/display-tiles]))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
