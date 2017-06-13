(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(def top-tile-state-atom (atom nil))
(def all-tile-states-atom (atom []))

(defn tile
  [state]
  (let [{:keys [parent-tile-state child-tile-states display title content]} @state]
    (.log js/console (pr-str (count child-tile-states) display title))
    (if (not display)
      nil
      [:table
       {:style {:float "left"}}
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
            " X"]]]]
        [:tr
         {:style {:background-color "Cornsilk"}}
         [:td
          {:style {:padding "5px"}}
          (if (some? content)
            (content state)
            nil)]]]])))

(defn basic-tile-state
  [title content]
  (let [tile-state-atom (atom {:child-tile-states []
                               :title title
                               :content content
                               :display false})
        tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1)]
    (swap! tile-state-atom assoc :tile-ndx tile-ndx)
    tile-state-atom))

(defn list-tile-state
  [title]
  (let [tile-state-atom (atom
                          {:child-tile-states []
                           :title title
                           :content (fn [state]
                                      (reduce
                                        (fn [v s]
                                          (let [d @s
                                                t (:title d)
                                                checked (:display d)]
                                            (conj v [:div
                                                     [:input {:type "checkbox"
                                                              :checked (true? checked)
                                                              :on-change (fn
                                                                           []
                                                                           (swap! s (fn [d] (assoc d :display (not (true? checked)))))
                                                                           (.log js/console (pr-str (:title @s) (:display @s))))}]
                                                     t])))
                                        [:dev]
                                        (:child-tile-states @state)))
                           :display false})
        tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1)]
    (swap! tile-state-atom assoc :tile-ndx tile-ndx)
    tile-state-atom))

(defn add-child-tile
  [parent-tile-state child-tile-state]
  (swap! child-tile-state assoc :parent-tile-state-ndx (:tile-ndx parent-tile-state))
  (swap! parent-tile-state (fn [d] (assoc d :child-tile-states (conj (:child-tile-states d) child-tile-state)))))

(defn tile-states
  [tile-state]
  (if (empty? (:child-tile-states @tile-state))
    [tile-state]
    (let [x (reduce
              (fn [v s]
                (.log js/console (pr-str (:title @s) (count (:child-tile-states @s)) (:display @s)))
                (if (not (true? (:display @s)))
                  v
                  (conj v (tile-states s))))
              [tile-state]
              (:child-tile-states @tile-state))]
      x)))

(defn display-tiles
  [state]
  ;(.log js/console (pr-str (count (tile-states state))))
  (reduce
    (fn [v s]
      (.log js/console (pr-str :hey (:title @s)))
      (let [i [tile s]]
        (if (nil? i)
          v
          (conj v i))))
    [:div]
    (tile-states state)))

(defn calling-component
  []
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
                                                 (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]))
        b2 (basic-tile-state
             "Test2"
             (fn [state]
               [:div 222]))
        ]
    (add-child-tile l1 b1)
    (add-child-tile l1 b2)
    (reset! top-tile-state-atom l1)
    (swap! l1 (fn [d] (assoc d :display true)))
    (display-tiles l1)))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
