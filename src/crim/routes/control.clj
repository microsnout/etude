(ns crim.routes.control
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.models.db :as db]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [noir.session :as session]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [claudio.id3 :as id3]
            ))


(defn control []
  (layout/common 
      :include-js "js/site.js"
      [:br]
      [:div#info-line]
      [:div#textbox [:div#display.scroll-pane]]

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
    :playIndex   0
    :audioPath   "data/crim/audio/"
    :textPath    "resources/public/data/crim/text/"
    :audioExt    ".m4a"
    :textExt     ".txt"
    :fileList    ["01_01" "01_02" "10_01" "01_10"]
    :id3Title    false
    })

(defn scan-dataset [name]
  (let [root (str "resources/public/data/" name)
        dset (slurp (str root "/dataset.json"))
        json (st/replace dset #"//.*\n" "\n")
        spec (json/read-str json :key-fn keyword)
        text (str root "/text")
        list (.list (io/file text))
        names (map (fn [s] (st/replace-first s (:textExt spec) "")) list)]

    {
      :playIndex  0
      :audioPath  (str "data/" name "/audio/")
      :textPath   (str "resources/public/data/" name "/text/")
      :audioExt   (:audioExt spec) 
      :textExt    (:textExt spec) 
      :fileList   (shuffle names)
      :id3Title   (:id3Title spec)
    }
  )
)


(def data-set (scan-dataset "offqc"))
;;(defonce data-set (future (scan-dataset "offqc")))

;; Client event handlers

(defn gen-cmd-resp []
  (let [st (session/get :state)
        px (:playIndex st)
        fn ((:fileList st) px)
        tp (str (:textPath st) fn (:textExt st))
        ap (str (:audioPath st) fn (:audioExt st))
        io (clojure.java.io/file (str "resources/public/" ap))
        il (if (:id3Title st) (:title (id3/read-tag io)) fn)]
    [
      [:loadText tp]
      [:loadAudio ap]
      [:setInfoLine il]
    ]
  )
)


(defn event-startup []
  ;; Turn off debugging output from claudio
  (.setLevel (java.util.logging.Logger/getLogger "org.jaudiotagger")
           java.util.logging.Level/OFF)

  (if (empty? (session/get :state))
    (session/put! :state data-set))
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
