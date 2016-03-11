(ns crim.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [image link-to]]
            [noir.session :as session]))


(defn get-login-status []
  (if-let [id (session/get :user)]
    (list "User: " [:span.username id] " " (link-to {:class "footText"} "/logout" "Logout"))
    (list "User: -----" (link-to {:class "footText"} "/login" "Login"))
  )
)


(defn get-header []
  [:header {:width "100%"}
   [:nav#lognav.dark-back
      (get-login-status)
   ]
   [:div#banner.header-box
      [:div#site-logo {:height "100px"}
        "étude"]
      [:div#flag
        (image {:width "64", :height "64", :float 'right'} "/img/drapeau_Quebec.gif")]
      [:nav#boxnav
         [:ul.row-list.navrow
            [:li (link-to "/" "Home")] 
            [:li (link-to "/player" "Player")]
            [:li (link-to "/words" "Words")]
            [:li (link-to "/guestbook" "Guestbook")] ]]
   ]
  ]
)


(defn get-footer []
  [:footer.dark-back
    [:div.footBanner "étude version 0.1"]
    [:nav.footMenu
       [:ul.row-list
          [:li (link-to {:class "footText"} "/guestbook" "Guestbook")]
          [:li (link-to {:class "footText"} "/userlist" "Users")]
       ]]
  ]
)


(defn common [& main]
  (if-let [[keyw filespec & tail] (and (= (first main) :include-js) main)]
    (html5
      [:head
         [:title "crim 0.1"]
         (include-css "/css/jquery.jscrollpane.css")
         (include-css "/css/jquery.jscrollpane.mytheme.css")
         (include-css "/css/fonts/stylesheet.css")
         (include-css "/css/screen.css")
         (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.12.0/jquery.min.js")
         (include-js "/js/jquery.jscrollpane.min.js")
         (include-js "/js/jquery.mousewheel.js")
         (include-js "/js/jquery.json.js")
         (include-js filespec)]
      [:body (get-header) [:main tail] (get-footer)]
    )
    (html5
      [:head
         [:title "crim 0.1"]
         (include-css "/css/jquery.jscrollpane.css")
         (include-css "/css/jquery.jscrollpane.mytheme.css")
         (include-css "/css/fonts/stylesheet.css")
         (include-css "/css/screen.css")
         (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.12.0/jquery.min.js")]
         (include-js "/js/jquery.jscrollpane.min.js")
         (include-js "/js/jquery.mousewheel.js")
         (include-js "/js/jquery.json.js")
      [:body (get-header) [:main main] (get-footer)]
    )
  )
)
