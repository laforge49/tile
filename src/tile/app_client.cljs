(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn calling-component
      []
      [:div
       [:h1 "Tile example"]
       [:p "try hitting the button:"]
       [:p
        [:input {:type "button"
                 :value "chsk-send! (with reply)"
                 :on-click (fn []
                               (->output! "Button 2 was clicked (will receive reply from server)")
                               (chsk-send! [:example/button2 {:had-a-callback? "indeed"}] 5000
                                           (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]])

(defn start!
      []
      (client/start!)
      (reagent/render-component [calling-component]
                                (.getElementById js/document "container")))
