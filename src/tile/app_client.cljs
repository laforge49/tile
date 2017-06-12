(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(def top-tile-atom (atom nil))

(defn tile
      [state]
      (fn [state]
          (let [{:keys [parent-tile-state child-tile-states display title content]} @state]
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
      [title content]
      (let [tile-state (atom {:child-tile-state []
                              :title title
                              :content content})]
           tile-state))

(defn calling-component
      []
      [:div
       (let [b1 (basic-tile-state
                  "Basic tile example"
                  (fn []
                      [:div
                       [:input {:type "button"
                                :value "chsk-send! (with reply)"
                                :on-click (fn []
                                              (->output! "Button 2 was clicked (will receive reply from server)")
                                              (chsk-send! [:example/button2 {:had-a-callback? "indeed"}] 5000
                                                          (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]))]
            (reset! top-tile-atom b1)
            (swap! b1 (fn [d] (assoc d :display true)))
            [tile b1])])

(defn start!
      []
      (client/start!)
      (reagent/render-component [calling-component]
                                (.getElementById js/document "container")))
