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
                       (content state)
                       nil)]]]]))))

(defn basic-tile-state
      [title content]
      (let [tile-state (atom {:child-tile-states []
                              :title title
                              :content content})]
           tile-state))

(defn list-tile-state
      [title]
      (let [tile-state (atom {:child-tile-states []
                              :title title
                              :content (fn [state]
                                           [:dev (count (:child-tile-states @state))])})]
           tile-state))

(defn add-child-tile
      [parent-tile-state child-tile-state]
      (swap! child-tile-state (fn [d] (assoc d :parent-tile-state parent-tile-state)))
      (swap! parent-tile-state (fn [d] (assoc d :child-tile-states (conj (:child-tile-states d) child-tile-state)))))

(defn calling-component
      []
      [:div
       (let [l1 (list-tile-state "Basic tile example")
             b1 (basic-tile-state
                  "Test"
                  (fn [state]
                      [:div
                       [:input {:type "button"
                                :value "chsk-send! (with reply)"
                                :on-click (fn []
                                              (->output! "Button 2 was clicked (will receive reply from server)")
                                              (chsk-send! [:example/button2 {:had-a-callback? "indeed"}] 5000
                                                          (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]))]
            (add-child-tile l1 b1)
            (reset! top-tile-atom l1)
            (swap! l1 (fn [d] (assoc d :display true)))
            [tile l1])])

(defn start!
      []
      (client/start!)
      (reagent/render-component [calling-component]
                                (.getElementById js/document "container")))
