(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(def top-tile-state-atom (atom nil))
(def all-tile-states-atom (atom []))
(def selected-tile-ndx-atom (atom 0))

(defn close-tile
  [tile-ndx]
  (let [tile-state-atom (nth @all-tile-states-atom tile-ndx)
        child-tile-ndxes (:child-tile-ndxes @tile-state-atom)]
    (if (and
          (> tile-ndx 0)
          (:display @tile-state-atom))
      (do
        (reduce
          (fn [o ndx]
            (close-tile ndx))
          nil
          child-tile-ndxes)
        (swap! tile-state-atom assoc :display false)
        (if (= tile-ndx @selected-tile-ndx-atom)
          (reset! selected-tile-ndx-atom (:parent-tile-ndx @tile-state-atom)))))))

(defn tile
  [state]
  (let [{:keys [parent-tile-ndx child-tile-ndxes display title content tile-ndx]} @state]
    (if (not display)
      nil
      [:table
       {:style {:border (if (= @selected-tile-ndx-atom tile-ndx)
                          "5px solid blue"
                          "5px solid lime")
                :float "left"}}
       [:tbody
        [:tr
         [:td
          {:style {:background-color "yellow"}}
          [:div
           [:div
            {:style {:float "left"
                     :padding "5px"}}
            (if (> tile-ndx 0)
              [:a
               {:on-click #(reset! selected-tile-ndx-atom parent-tile-ndx)
                :style {:cursor "pointer"}}
               [:strong "^"]])
            " "
            [:a
             {:on-click #(reset! selected-tile-ndx-atom tile-ndx)
              :style {:cursor "pointer"}}
             [:strong (str title " ")]]]
           (if (> tile-ndx 0)
             [:div
              {:style {:float "right"}}
              [:input {:disabled (= tile-ndx 0)
                       :type "button"
                       :value "X"
                       :on-click #(close-tile tile-ndx)}]])]]]
        [:tr
         {:style {:background-color "Cornsilk"}}
         [:td
          {:style {:padding "5px"}}
          (if (some? content)
            (content state)
            nil)]]]])))

(defn basic-tile-state-atom
  [title content]
  (let [tile-state-atom (atom {:child-tile-ndxes []
                               :title title
                               :content content
                               :display false})
        tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1)]
    (swap! tile-state-atom assoc :tile-ndx tile-ndx)
    tile-state-atom))

(defn list-tile-state-atom
  [title]
  (let [tile-state-atom (atom
                          {:child-tile-ndxes []
                           :title title
                           :content (fn [state]
                                      (reduce
                                        (fn [v ndx]
                                          (let [s (nth @all-tile-states-atom ndx)]
                                            (conj v [:div
                                                     [:input {:type "checkbox"
                                                              :checked (true? (:display @s))
                                                              :on-change (fn []
                                                                           (if (true? (:display @s))
                                                                             (close-tile ndx)
                                                                             (do
                                                                               (swap! s assoc :display true)
                                                                               (reset! selected-tile-ndx-atom ndx))))}]
                                                     (:title @s)])))
                                        [:dev]
                                        (:child-tile-ndxes @state)))
                           :display false})
        tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1)]
    (swap! tile-state-atom assoc :tile-ndx tile-ndx)
    tile-state-atom))

(defn add-child-tile
  [parent-tile-state-atom child-tile-state-atom]
  (swap! child-tile-state-atom assoc :parent-tile-ndx (:tile-ndx @parent-tile-state-atom))
  (swap! parent-tile-state-atom (fn [d] (assoc d :child-tile-ndxes (conj (:child-tile-ndxes d) (:tile-ndx @child-tile-state-atom))))))

(defn tile-states
  [tile-state]
  (if (empty? (:child-tile-ndxes @tile-state))
    [tile-state]
    (let [x (reduce
              (fn [v ndx]
                (let [s (nth @all-tile-states-atom ndx)]
                  (into v (tile-states s))))                ;)
              [tile-state]
              (:child-tile-ndxes @tile-state))]
      x)))

(defn display-tiles
  [state]
  (reduce
    (fn [v s]
      (let [i [tile s]]
        (if (nil? i)
          v
          (conj v i))))
    [:div]
    (tile-states state)))

(defn calling-component
  []
  (let [l1 (list-tile-state-atom "Basic tile example")
        l2 (list-tile-state-atom "sub-list")
        b1 (basic-tile-state-atom
             "Test"
             (fn [state]
               [:div
                [:input {:type "button"
                         :value "chsk-send! (with reply)"
                         :on-click (fn []
                                     (->output! "Button 2 was clicked (will receive reply from server)")
                                     (chsk-send! [:example/button2 {:had-a-callback? "indeed"}] 5000
                                                 (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]))
        b2 (basic-tile-state-atom
             "Test2"
             (fn [state]
               [:div 222]))
        ]
    (add-child-tile l1 l2)
    (add-child-tile l1 b1)
    (add-child-tile l2 b2)
    (reset! top-tile-state-atom l1)
    (swap! l1 assoc :display true)
    (fn []
      [display-tiles l1])))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
