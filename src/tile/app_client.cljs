(ns tile.app-client
  (:require [tile.client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn calling-component
      []
      [:div
       [:h1 "Tile example"]
       [:p [:strong "Step 1: "] " try hitting the buttons:"]
       [:p
        [:input {:type "button"
                 :value "chsk-send! (w/o reply)"
                 :on-click (fn []
                               (->output! "Button 1 was clicked (won't receive any reply from server)")
                               (chsk-send! [:example/button1 {:had-a-callback? "nope"}]))}]
        [:input {:type "button"
                 :value "chsk-send! (with reply)"
                 :on-click (fn []
                               (->output! "Button 2 was clicked (will receive reply from server)")
                               (chsk-send! [:example/button2 {:had-a-callback? "indeed"}] 5000
                                           (fn [cb-reply] (->output! "Callback reply: %s" cb-reply))))}]]
       [:p
        [:input {:type "button"
                 :value "Test rapid server>user async pushes"
                 :on-click (fn []
                               (->output! "Button 3 was clicked (will ask server to test rapid async push)")
                               (chsk-send! [:example/test-rapid-push]))}]
        [:input {:type "button"
                 :value "Toggle server>user async broadcast push loop"
                 :on-click (fn []
                               (->output! "Button 4 was clicked (will toggle async broadcast loop)")
                               (chsk-send! [:example/toggle-broadcast] 5000
                                           (fn [cb-reply]
                                               (when (cb-success? cb-reply)
                                                     (let [loop-enabled? cb-reply]
                                                          (if loop-enabled?
                                                            (->output! "Async broadcast loop now enabled")
                                                            (->output! "Async broadcast loop now disabled")))))))}]]
       [:p
        [:input {:type "button"
                 :value "Disconnect"
                 :on-click (fn []
                               (->output! "Disconnecting")
                               (sente/chsk-disconnect! chsk))}]
        [:input {:type "button"
                 :value "Reconnect"
                 :on-click (fn []
                               (->output! "Reconnecting")
                               (sente/chsk-reconnect! chsk))}]]

       ])

(defn calling-component2
      []
      [:div
       [:hr]
       [:h2 "Step 3: try login with a user-id"]
       [:p "The server can use this id to send events to *you* specifically."]
       [:p
        [:input {:id "input-login"
                 :type :text
                 :placeholder "User-id"}]
        [:input {:type "button"
                 :value "Secure login!"
                 :on-click (fn []
                               (let [user-id (.-value (.getElementById js/document "input-login"))]
                                    (if (str/blank? user-id)
                                      (js/alert "Please enter a user-id first")
                                      (do
                                        (->output! "Logging in with user-id %s" user-id)

                                        ;;; Use any login procedure you'd like. Here we'll trigger an Ajax
                                        ;;; POST request that resets our server-side session. Then we ask
                                        ;;; our channel socket to reconnect, thereby picking up the new
                                        ;;; session.

                                        (sente/ajax-lite "/login"
                                                         {:method :post
                                                          :headers {:X-CSRF-Token (:csrf-token @chsk-state)}
                                                          :params {:user-id (str user-id)}}

                                                         (fn [ajax-resp]
                                                             (->output! "Ajax login response: %s" ajax-resp)
                                                             (let [login-successful? true ; Your logic here
                                                                   ]
                                                                  (if-not login-successful?
                                                                          (->output! "Login failed")
                                                                          (do
                                                                            (->output! "Login successful")
                                                                            (sente/chsk-reconnect! chsk))))))))))}]]
       ])

(defn start!
      []
      (client/start!)
      (reagent/render-component [calling-component]
                                (.getElementById js/document "container"))
      (reagent/render-component [calling-component2]
                                (.getElementById js/document "container2")))
