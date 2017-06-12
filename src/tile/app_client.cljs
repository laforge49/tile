(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn tile
      [state]
      (fn [state]
          (let [{:keys [parent-tile-state display title content]} @state]
               (if (not display)
                 nil
                 [:table
                  [:tbody
                   {:style {:border "5px solid red" :float "left"}}
                   [:tr
                    {:style {:background-color "yellow"}}
                    [:td
                     {:style {:padding "5px"}}
                     [:div
                      [:div
                       {:style {:float "left"}}
                       [:strong (str title " ")]]
                      [:div
                       {:style {:float "right"}}
                       "X"]]]]
                   [:tr
                    {:style {:background-color "Cornsilk"}}
                    [:td
                     {:style {:padding "5px"}}
                     (if (some? content)
                       (content)
                       nil)]]]]))))

(defn basic-tile-state
      [parent-tile-state title content]
      (atom {:parent-tile-state parent-tile-state
             :display true
             :title title
             :content content}))

(defn calling-component
      []
      [:div
       [tile (basic-tile-state
               nil
               "Basic tile example"
               (fn []
                   [:div
                    [:input {:type "button"
                             :value "chsk-send! (with reply)"
                             :on-click (fn []
                                           (->output! "Button 2 was clicked (will receive reply from server)")
                                           (chsk-send! [:example/button2 {:had-a-callback? "indeed"}] 5000
                                                       (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]))]])

(defn start!
      []
      (client/start!)
      (reagent/render-component [calling-component]
                                (.getElementById js/document "container")))
