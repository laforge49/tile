(ns tile.core
  (:require [reagent.core :as reagent :refer [atom]]))

(def all-tile-states-atom (atom []))
(def selected-tile-ndx-atom (atom 0))

(defn select-tile
  [tile-ndx]
  (reset! selected-tile-ndx-atom tile-ndx)
  (.scrollIntoView (.getElementById js/document (str "tile" tile-ndx))))

(defn close-tile
  ([tile-ndx]
   (close-tile tile-ndx true))
  ([tile-ndx root]
   (let [tile-state-atom (nth @all-tile-states-atom tile-ndx)
         children (:children @tile-state-atom)
         children (if (some? children)
                    children
                    [])
         parent-tile-ndx (:parent-tile-ndx @tile-state-atom)
         parent-state-atom (if (> tile-ndx 0)
                             (nth @all-tile-states-atom parent-tile-ndx)
                             nil)]
     (reduce
       (fn [o child-ndx]
         (let [child (nth children child-ndx)
               ndx-atom (:ndx-atom child)]
           (when (> @ndx-atom -1)
             (reset! ndx-atom -1)
             (close-tile @ndx-atom false)
             )))
       nil
       (range (count children)))
     (swap! tile-state-atom assoc :display false)
     (if (= tile-ndx @selected-tile-ndx-atom)
       (select-tile parent-tile-ndx))
     (if root
       (let [children (:children @parent-state-atom)
             ndx-atom (reduce
                         (fn [ndx-atom child-ndx]
                           (if (some? ndx-atom)
                             ndx-atom
                             (let [child (nth children child-ndx)]
                               (if (= tile-ndx @(:ndx-atom child))
                                 (:ndx-atom child)
                                 nil))))
                         nil
                         (range (count children)))]
         (reset! ndx-atom -1))))))

(defn tile
  [state-atom]
  (let [{:keys [parent-tile-ndx child-tile-ndxes display title content tile-ndx]} @state-atom]
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
             (content state-atom)
             nil)]]]])]))

(defn locate-undisplayed
  []
  (reduce
    (fn [r ndx]
      (if (> r -1)
        r
        (if (:display @(nth @all-tile-states-atom ndx))
          -1
          ndx)))
    -1
    (range (count @all-tile-states-atom))))

(defn register-state
  [tile-state]
  (let [ndx (locate-undisplayed)]
    (if (= ndx -1)
      (let [tile-state-atom (atom tile-state)]
        (swap! tile-state-atom assoc :tile-ndx (- (count (swap! all-tile-states-atom conj tile-state-atom)) 1))
        tile-state-atom)
      (let [tile-state (assoc tile-state :tile-ndx ndx)
            tile-state-atom (nth @all-tile-states-atom ndx)]
        (reset! tile-state-atom tile-state)
        tile-state-atom))))

(defn create-tile-state-atom
  [make title]
  (let [tile-state (make)
        tile-state (assoc tile-state :title title)
        tile-state (assoc tile-state :display true)]
    (register-state tile-state)))

(defn basic-tile-state
  [content]
  {:content content})

(defn list-tile-state
  [children]
  {:children children
   :display false
   :content
   (fn [state-atom]
     (reduce
       (fn [v child-ndx]
         (let [child (nth (:children @state-atom) child-ndx)
               make (:make child)
               title (:title child)
               ndx-atom (:ndx-atom child)
               ndx @ndx-atom
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
                                    (let [sa (create-tile-state-atom make title)
                                          tile-ndx (:tile-ndx @sa)]
                                      (swap! sa assoc :parent-tile-ndx (:tile-ndx @state-atom))
                                      (reset! ndx-atom tile-ndx)
                                      (.setTimeout js/window
                                                   #(select-tile tile-ndx)
                                                   0)))
                        :style {:cursor "pointer"}}
                       ">"]]))))
       [:dev]
       (range (count (:children @state-atom)))))})

(declare display-map)

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

(defn map-tile-state
  ([ifn m]
   (map-tile-state ifn (into [] (into (sorted-map) m)) [] false [:div] m))
  ([ifn index path p v m]
   {:content
    (fn [state-atom]
      (display-map ifn index path p v m))}))

(defn display-tiles
  []
  (reduce
    (fn [v s]
      (conj v [tile s]))
    [:div]
    @all-tile-states-atom))
