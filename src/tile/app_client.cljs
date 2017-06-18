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

(defn make-l2
  []
  (tile/list-tile-state-atom
    [{:make make-b2 :title "Test2" :ndx -1}]))

(defn make-l1
  []
  (tile/list-tile-state-atom
    [{:make make-b1 :title "Test" :ndx -1}
     {:make make-m1 :title "Basic map" :ndx -1}
     {:make make-m2 :title "Map2" :ndx -1}
     {:make make-l2 :title "Sub-list" :ndx -1}]))

(defn calling-component
  []
  (tile/create-tile-state
    make-l1
    "Basic tile example")
  (fn []
    [tile/display-tiles]))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
