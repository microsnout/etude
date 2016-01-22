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
     [:h1 "Control Center"]
     [:br]

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


(defroutes control-routes
 (GET "/control" [] (control))
 (GET "/userlist" [] (userlist))
)
