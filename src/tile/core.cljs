(ns tile.core
  (:require [reagent.core :as reagent :refer [atom]]))

(def all-tile-states-atom (atom []))
(def selected-tile-ndx-atom (atom 0))

(defn select-tile
  [tile-ndx]
  (reset! selected-tile-ndx-atom tile-ndx)
  (.scrollIntoView (.getElementById js/document (str "tile" tile-ndx))))
