(ns tile.core
  (:require [reagent.core :as reagent :refer [atom]]))

(def all-tile-states-atom (atom []))
(def selected-tile-ndx-atom (atom 0))

(defn select-tile
  [tile-ndx]
  (reset! selected-tile-ndx-atom tile-ndx)
  (.scrollIntoView (.getElementById js/document (str "tile" tile-ndx))))

(defn close-tile
  [tile-ndx]
  (let [tile-state-atom (nth @all-tile-states-atom tile-ndx)
        children (:children @tile-state-atom)
        children (if (some? children)
                   children
                   [])]
    (if (and
          (> tile-ndx 0)
          (:display @tile-state-atom))
      (do
        (reduce
          (fn [o child]
            (let [ndx (:ndx child)]
              (if (> ndx -1)
                (close-tile ndx))))
          nil
          children)
        (swap! tile-state-atom assoc :display false)
        (if (= tile-ndx @selected-tile-ndx-atom)
          (select-tile (:parent-tile-ndx @tile-state-atom)))))))

(defn tile
  [state]
  (let [{:keys [parent-tile-ndx child-tile-ndxes display title content tile-ndx]} @state]
    [:div {:id (str "tile" tile-ndx)}
     (if (not display)
       nil
       [:table
        {:style {:border (if (= @selected-tile-ndx-atom tile-ndx)
                           "5px solid blue"
                           "5px solid lime")
                 ;:float "left"
                 }}
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
                {:on-click #(select-tile parent-tile-ndx)
                 :style {:cursor "pointer"}}
                [:strong "^"]])
             " "
             [:a
              {:on-click #(select-tile tile-ndx)
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
             nil)]]]])]))

(defn create-tile-state
  [make title]
  (let [tile-state-atom (make)]
    (swap! tile-state-atom assoc :title title)
    (swap! tile-state-atom assoc :tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1))
    (swap! tile-state-atom assoc :display true)
    tile-state-atom))

(defn basic-tile-state-atom
  [content]
  (atom {:content content}))

(defn list-tile-state-atom
  [children]
  (let [tile-state-atom
        (atom
          {:children children
           :display false
           :content
           (fn [state]
             (reduce
               (fn [v child-ndx]
                 (let [child (nth (:children @state) child-ndx)
                       make (:make child)
                       title (:title child)
                       ndx (:ndx child)
                       s (if (> ndx -1)
                           (nth @all-tile-states-atom ndx)
                           nil)]
                   (conj v (if (some? s)
                             [:div
                              [:a
                               {:on-click #(select-tile ndx)
                                :style {:cursor "pointer"}}
                               title]
                              " "
                              [:a
                               {:on-click #(close-tile ndx)
                                :style {:cursor "pointer"}}
                               "<"]]
                             [:div
                              title
                              " "
                              [:a
                               {:on-click (fn []
                                            (let [s (create-tile-state make title)
                                                  ndx (:tile-ndx @s)]
                                              (swap! s assoc :parent-tile-ndx (:tile-ndx @state))
                                              (swap! state assoc-in [:children child-ndx :ndx] ndx)
                                              (.setTimeout js/window
                                                           #(select-tile ndx)
                                                           0)))
                                :style {:cursor "pointer"}}
                               ">"]]))))
               [:dev]
               (range (count (:children @state)))))})]
    tile-state-atom))

(declare display-map map-tile-state-atom)

(defn eval
  [k ifn path]
  (let [i (k ifn)]
    (if (nil? i)
      nil
      (i path))))

(defn ordered
  [k ifn path m]
  (let [index (eval k ifn path)]
    (if (nil? index)
      (into [] (into (sorted-map) m))
      (reduce
        (fn [v e]
          [(first (val e)) ((first (val e)) m)])
        []
        index))))

(defn sub-display
  [ifn path p k m]
  (let [plus-atm (atom p)
        star-atm (atom p)]
    (fn [ifn path p k m]
      (if @plus-atm
        [:div
         (str k " ")
         [:a
          {:style {:cursor "pointer" :color "blue"}
           :on-click (fn []
                       (reset! plus-atm false)
                       (reset! star-atm false))}
          [:strong "="]]
         " {"
         [:br]
         [:div {:style {:padding-left "1em"}}
          (display-map ifn (ordered k ifn path m) path @star-atm [:div] m)]
         "}"]
        [:div (str k " ")
         [:a
          {:style {:cursor "pointer" :color "blue"}
           :on-click (fn []
                       (reset! plus-atm true))}
          [:strong "+"]]
         [:a
          {:style {:cursor "pointer" :color "blue"}
           :on-click (fn []
                       (reset! plus-atm true)
                       (reset! star-atm true))}
          [:strong "*"]]
         ]))))

(defn display-map
  [ifn order path p v m]
  (reduce
    (fn [v e]
      (let [ke (first e)
            value (second e)
            path (conj path ke)]
        (if (map? value)
          (conj v [sub-display ifn path p ke value])
          (conj v [:div (str ke " = " (pr-str value))]))))
    v
    order))

(defn map-tile-state-atom
  ([ifn m]
   (map-tile-state-atom ifn (into [] (into (sorted-map) m)) [] false [:div] m))
  ([ifn index path p v m]
   (let [tile-state-atom
         (atom {:content
                (fn [state]
                  (display-map ifn index path p v m))})]
     tile-state-atom)))

(defn display-tiles
  []
  (reduce
    (fn [v s]
      (conj v [tile s]))
    [:div]
    @all-tile-states-atom))
