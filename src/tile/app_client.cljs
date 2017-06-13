(ns tile.app-client
  (:require [tile.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(def top-tile-state-atom (atom nil))
(def all-tile-states-atom (atom []))

(defn tile
  [state]
  (let [{:keys [parent-tile-state child-tile-ndxes display title content]} @state]
    (.log js/console (pr-str (count child-tile-ndxes) display title))
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

(defn basic-tile-state-atom
  [title content]
  (let [tile-state-atom (atom {:child-tile-ndxes []
                               :title title
                               :content content
                               :display false})
        tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1)]
    #_(.log js/console (pr-str :basic tile-ndx))
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
                                          (let [s (nth @all-tile-states-atom ndx)
                                                d @s
                                                t (:title d)
                                                checked (:display d)]
                                            (conj v [:div
                                                     [:input {:type "checkbox"
                                                              :checked (true? checked)
                                                              :on-change (fn
                                                                           []
                                                                           (swap! s assoc :display (not (true? checked)))
                                                                           (.log js/console (pr-str (:title @s) (:display @s))))}]
                                                     t])))
                                        [:dev]
                                        (:child-tile-ndxes @state)))
                           :display false})
        tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1)]
    (swap! tile-state-atom assoc :tile-ndx tile-ndx)
    tile-state-atom))

(defn add-child-tile
  [parent-tile-state-atom child-tile-state-atom]
  #_(.log js/console (pr-str :add (:tile-ndx @parent-tile-state-atom) (:tile-ndx @child-tile-state-atom)))
  (swap! child-tile-state-atom assoc :parent-tile-ndx (:tile-ndx @parent-tile-state-atom))
  (swap! parent-tile-state-atom (fn [d] (assoc d :child-tile-ndxes (conj (:child-tile-ndxes d) (:tile-ndx @child-tile-state-atom))))))

(defn tile-states
  [tile-state]
  (if (empty? (:child-tile-ndxes @tile-state))
    [tile-state]
    (let [x (reduce
              (fn [v ndx]
                (.log js/console (pr-str :ndx ndx))
                (let [s (nth @all-tile-states-atom ndx)]
                  (.log js/console (pr-str (:title @s) (count (:child-tile-ndxes @s)) (:display @s)))
                  (if (not (true? (:display @s)))
                    v
                    (conj v (tile-states s)))))
              [tile-state]
              (:child-tile-ndxes @tile-state))]
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
  (let [l1 (list-tile-state-atom "Basic tile example")
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
    (add-child-tile l1 b1)
    (add-child-tile l1 b2)
    (reset! top-tile-state-atom l1)
    (swap! l1 assoc :display true)
    (display-tiles l1)))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
