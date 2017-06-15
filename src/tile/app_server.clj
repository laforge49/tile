(ns tile.app-server
    (:require [tile.sente-server :as server]))


(defn -main "For `lein run`, etc." []
      (server/start! 3001))
