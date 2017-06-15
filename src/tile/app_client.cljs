(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]
            [tile.core :as tile]))

(defn calling-component
  []
  (let [l1 (tile/list-tile-state-atom "Basic tile example")
        l2 (tile/list-tile-state-atom "sub-list")
        b1 (tile/basic-tile-state-atom
             "Test"
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
                                         (->output! "Callback reply: %s" cb-reply))))}]]))
        b2 (tile/basic-tile-state-atom
             "Test2"
             (fn [state]
               [:div 222]))
        m1 (tile/map-tile-state-atom
             "basic map"
             {}
             {:a 1 :b 2 :c 3})
        m2 (tile/map-tile-state-atom
             "map2"
             {}
             {:a 1 :b {:x 55 :y :apples} :c 3})
        ]
    (tile/add-child-tile l1 m1)
    (tile/add-child-tile l1 m2)
    (tile/add-child-tile l1 l2)
    (tile/add-child-tile l1 b1)
    (tile/add-child-tile l2 b2)
    (swap! l1 assoc :display true)
    (fn []
      [tile/display-tiles l1])))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
