(ns crim.routes.control
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.routes.auth :as auth]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [crim.models.db :as db]
            [noir.session :as session]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))


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


;; Dataset Access funtions

(def test-state
  {
    :playIndex   0
    :audioPath   "data/crim/audio/"
    :textPath    "resources/public/data/crim/text/"
    :audioExt    ".m4a"
    :textExt     ".txt"
    :fileList    ["01_01" "01_02"]
    })

(defn scan-dataset [name]
  (let [path (str "resources/public/data/"  name  "/text")
        list (.list (io/file path))
        names (map (fn [s] (st/replace-first s ".txt" "")) list)]

    {
      :playIndex 0
      :audioPath (str "data/" name "/audio/")
      :textPath  (str "resources/public/data/" name "/text/")
      :audioExt  ".m4a"
      :textExt   ".txt"
      :fileList  names
    }
  )
)

(defonce data-set (future (scan-dataset "crim")))


(defn ctl-get-text [req]
  (slurp (:url req))
)



(defn ctl-get-user-state []
  (json/write-str data-set))


(defroutes control-routes
  (GET "/ctl-get-text" request (ctl-get-text (:params request)))
  (GET "/ctl-get-user-state" [] (ctl-get-user-state))
  (GET "/control" [] (control))
  (GET "/userlist" [] (userlist))
)
