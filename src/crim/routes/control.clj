(ns crim.routes.control
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.routes.auth :as auth]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [crim.models.db :as db]
            [noir.session :as session]))


(defn control []
  (layout/common
     [:br]
     [:div#textbox]
     [:div#controls
        
          [:ul.ctl-list
            (map
              (fn [id chr]
                  [:li [:a {:role "button", :href "#", :id id} chr]])
              ["stop" "back" "play-pause" "replay" "next" "loop"]
              (map str (seq "37198(")))] 

        [:span.vert-split]
             
        [:div.audio-player-progress
          [:div.audio-player-progress-bar]]
      ]
  )
)

(defn userlist []
  (layout/common
     [:h1 "Users"]
     [:ul.guests
       (for [{:keys [userid pass]} (db/read-users)]
          [:li userid "   (" pass ")"])]
     [:br]
   )
)

(defn ctl-get-text []
  (str
  (slurp "resources/public/data/crim/text/01_05.txt")
  " <input type=\"text\" size=12> "
  (slurp "resources/public/data/crim/text/01_07.txt")))


(defroutes control-routes
  (GET "/ctl-get-text" [] (ctl-get-text))
  (GET "/control" [] (control))
  (GET "/userlist" [] (userlist))
)
