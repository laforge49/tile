(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn calling-component
      []
      [:div
       [:h1 "Tile example"]
       [:p [:strong "Step 1: "] " try hitting the buttons:"]
       [:p
        [:input {:type "button"
                 :value "chsk-send! (w/o reply)"
                 :on-click (fn []
                               (->output! "Button 1 was clicked (won't receive any reply from server)")
                               (chsk-send! [:example/button1 {:had-a-callback? "nope"}]))}]
        [:input {:type "button"
                 :value "chsk-send! (with reply)"
                 :on-click (fn []
                               (->output! "Button 2 was clicked (will receive reply from server)")
                               (chsk-send! [:example/button2 {:had-a-callback? "indeed"}] 5000
                                           (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]
       [:p
        [:input {:type "button"
                 :value "Test rapid server>user async pushes"
                 :on-click (fn []
                               (->output! "Button 3 was clicked (will ask server to test rapid async push)")
                               (chsk-send! [:example/test-rapid-push]))}]]
       [:p
        [:input {:type "button"
                 :value "Disconnect"
                 :on-click (fn []
                               (->output! "Disconnecting")
                               (sente/chsk-disconnect! chsk))}]
        [:input {:type "button"
                 :value "Reconnect"
                 :on-click (fn []
                               (->output! "Reconnecting")
                               (sente/chsk-reconnect! chsk))}]]

       ])

(defn start!
      []
      (client/start!)
      (reagent/render-component [calling-component]
                                (.getElementById js/document "container")))
