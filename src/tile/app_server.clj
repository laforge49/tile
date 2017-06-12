(ns tile.app-server
    (:require [tile.server :as server]))


(defn -main "For `lein run`, etc." []
      (server/start!))
