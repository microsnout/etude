(ns crim.routes.control
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.models.db :as db]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [noir.session :as session]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))


(defn control []
  (layout/common 
      :include-js "js/site.js"
      [:br]
      [:div#info-line]
      [:div#textbox]

      [:div#controls        
        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "stop"} "3"]]
          [:li [:a {:role "button", :href "#", :id "back", :class "server"} "7"]]
          [:li [:a {:role "button", :href "#", :id "play-pause"} "1"]]
          [:li [:a {:role "button", :href "#", :id "replay"} "9"]]
          [:li [:a {:role "button", :href "#", :id "next", :class "server"} "8"]]
          [:li [:a {:role "button", :href "#", :id "loop"} "("]]
        ]

        [:span.vert-split]
             
        [:div.audio-player-progress
          [:div.audio-player-progress-bar]]

        [:span.vert-split]
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
    :playMode    1
    :playIndex   0
    :audioPath   "data/crim/audio/"
    :textPath    "resources/public/data/crim/text/"
    :audioExt    ".m4a"
    :textExt     ".txt"
    :fileList    ["01_01" "01_02" "10_01" "01_10"]
    })

(defn scan-dataset [name]
  (let [path (str "resources/public/data/"  name  "/text")
        list (.list (io/file path))
        names (map (fn [s] (st/replace-first s ".txt" "")) list)]

    {
      :playMode   1
      :playIndex  0
      :audioPath  (str "data/" name "/audio/")
      :textPath   (str "resources/public/data/" name "/text/")
      :audioExt   ".m4a"
      :textExt    ".txt"
      :fileList   (shuffle names)
    }
  )
)


;; Client event handlers

(defn gen-cmd-resp []
  (let [st (session/get :state)
        px (:playIndex st)
        fn ((:fileList st) px)]
    [
      [:loadText
          (str (:textPath st) fn (:textExt st))]
      [:loadAudio
          (str (:audioPath st) fn (:audioExt st))]
    ]
  )
)


(defn event-startup []
  (if (empty? (session/get :state))
    (session/put! :state test-state))
  (gen-cmd-resp)
)


(defn event-next []
  (let
    [st (session/get :state)
     px (:playIndex st)
     nx (mod (inc px) (count (:fileList st)))]
    (session/put! :state (assoc st :playIndex nx) )
    (gen-cmd-resp)
  )
)


(defn event-back []
  (let
    [st (session/get :state)
     px (:playIndex st)
     nx (mod (dec px) (count (:fileList st)))]
    (session/put! :state (assoc st :playIndex nx) )
    (gen-cmd-resp)
  )
)


;; ******


(def data-set (scan-dataset "crim"))
;;(defonce data-set (future (scan-dataset "crim")))


(defn ctl-get-text [req]
  (slurp (:url req))
)


(defn ctl-post-user-event [ params ]
  (json/write-str
    (case (:id params)
      "startup" (event-startup)
      "next"    (event-next)
      "back"    (event-back)
      "ended"   (event-next)
      [])
  )
)


(defn shutdown []
)


(defroutes control-routes
  (GET "/ctl-get-text" request (ctl-get-text (:params request)))
  (POST "/ctl-post-user-event" request (ctl-post-user-event (:params request)))
  (GET "/control" [] (control))
  (GET "/userlist" [] (userlist))
  (GET "/logout" []
       (shutdown)
       nil)
)
