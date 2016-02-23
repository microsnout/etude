(ns crim.routes.control
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.models.db :as db]
            [hiccup.page :refer [html5]]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [noir.session :as session]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            ))


(defn control []
  (layout/common 
      :include-js "js/site.js"
      [:br]
      [:div#info-line]
      [:div#textbox [:div#display.scroll-pane]]

      [:div#controls        
        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "add", :class "server"} "="]]
          [:li [:a {:role "button", :href "#", :id "sub", :class "server"} "-"]]
        ]

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

(def blank-user-state
  {
    :activeSessions   []
  })


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
      :setName    name
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


;; Find all dataset directories and scan their contents
;;
(defn find-datasets []
  (let [root (io/file "resources/public/data/")
        dirs (map #(.getName %) (filter #(.isDirectory %) (.listFiles root)))]
    (zipmap
      (map keyword dirs)
      (map scan-dataset dirs))
  )
)


(def data-sets (find-datasets))
;;(defonce data-set (future (scan-dataset "offqc")))


(defn get-control-html []
  (html5
    [:div.flexContainer
      [:div
        [:table.flexItem 
          [:colgroup 
            [:col.nameCol]
            [:col.sizeCol]]
          [:tr
            [:th "Name"]
            [:th "Size"]]
          (map
            (fn [ds]
              [:tr 
                {:data-name (:setName ds)}
                [:td (:setName ds)]
                [:td (count (:fileList ds))]])
            (vals data-sets))
        ]
      ]
      [:div
        [:form { :action "" } 
          [:b "Session Type"] [:br]
          [:input { :type "radio" :name "Activity" :value "review" :checked true } "Review"] 
          [:br]
          [:input { :type "radio" :name "Activity" :value "cloze"} "Cloze"]]
        ]
    ]
  )
)


;; Client event handlers

(defn gen-cmd-resp []
  (let [st (session/get :state)]
    [
      [:loadControlHtml]
    ]
  )
)


(defn event-startup []
  (if (empty? (session/get :state))
    (session/put! :state data-sets))

  (gen-cmd-resp)
)


(defn event-add []
  (let
    [st (session/get :state)]
  )
)


(defn event-sub []
  (let
    [st (session/get :state)]
  )
)
;; ******


(defn post-user-event [ params ]
  (json/write-str
    (case (:id params)
      "startup" (event-startup)
      "add"    (event-add)
      "sub"    (event-sub)
      [])
  )
)


(defn shutdown []
)


(defroutes control-routes
  (GET "/ctl-get-control-html" request (get-control-html))
  (POST "/ctl-post-user-event" request (post-user-event (:params request)))
  (GET "/control" [] (control))
  (GET "/userlist" [] (userlist))
;  (GET "/logout" []
;       (shutdown)
;       nil)
)
