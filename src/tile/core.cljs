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
  (atom {:child-tile-ndxes []
         :content content}))

(defn list-tile-state-atom
  []
  (let [tile-state-atom
        (atom
          {:child-tile-ndxes []
           :display false
           :content
           (fn [state]
             (reduce
               (fn [v ndx]
                 (let [s (nth @all-tile-states-atom ndx)]
                   (conj v [:div
                            [:input {:type "checkbox"
                                     :checked (true? (:display @s))
                                     :on-change
                                     (fn []
                                       (if (true? (:display @s))
                                         (close-tile ndx)
                                         (do
                                           (swap! s assoc :display true)
                                           (.setTimeout js/window
                                                        #(select-tile ndx)
                                                        0)
                                           )))}]
                            (if (true? (:display @s))
                              [:a
                               {:on-click #(select-tile ndx)
                                :style {:cursor "pointer"}}
                               (:title @s)]
                              (:title @s))])))
               [:dev]
               (:child-tile-ndxes @state)))})]
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
          {:style    {:cursor "pointer" :color "blue"}
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
          {:style    {:cursor "pointer" :color "blue"}
           :on-click (fn []
                       (reset! plus-atm true))}
          [:strong "+"]]
         [:a
          {:style    {:cursor "pointer" :color "blue"}
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
        (atom {:child-tile-ndxes []
               :content
               (fn [state]
                 (display-map ifn index path p v m))})]
    tile-state-atom)))

(defn add-child-tile
  [parent-tile-state-atom child-tile-state-atom]
  (swap! child-tile-state-atom assoc :parent-tile-ndx (:tile-ndx @parent-tile-state-atom))
  (swap! parent-tile-state-atom
         (fn [d]
           (assoc
             d
             :child-tile-ndxes
             (conj (:child-tile-ndxes d)
                   (:tile-ndx @child-tile-state-atom))))))

(defn display-tiles
  []
  (reduce
    (fn [v s]
      (conj v [tile s]))
    [:div]
    @all-tile-states-atom))
