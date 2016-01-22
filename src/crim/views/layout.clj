(ns crim.views.layout
  (:require [hiccup.page :refer [html5 include-css]]
            [hiccup.element :refer [image link-to]]
            [noir.session :as session]))


(defn get-login-status []
  (if-let [id (session/get :user)]
    (list "User: " [:span.username id] " " (link-to "/logout" "Logout"))
    (list "User: ----  " (link-to "/login" "Login"))
  )
)


(defn get-header []
  [:header {:width "100%"}
   [:nav#lognav
      (get-login-status)
   ]
   [:div#banner.head-box
      [:div#snout {:height "100px"}
        [:i "Franco Snout"]]
      [:div#flag
        (image {:width "64", :height "64", :float 'right'} "/img/drapeau_Quebec.gif")]
      [:nav#boxnav
         [:ul.row-list.navrow
            [:li (link-to "/" "Home")] [:li (link-to "/guestbook" "Guestbook")] ]]
   ]
   [:hr]
  ]
)


(defn get-footer []
  [:footer
    [:hr]
    [:div.footBanner "CRIM version 0.1"]
    [:nav.footMenu
       [:ul.row-list
          [:li (link-to {:class "footText"} "/guestbook" "Guestbook")]
          [:li (link-to {:class "footText"} "/userlist" "Users")]
       ]]
  ]
)


(defn common [& main]
  (html5
    [:head
       [:title "crim 0.1"]
       (include-css "/css/screen.css")]
    [:body (get-header) [:main main] (get-footer)]
  )
)


