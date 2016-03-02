(ns crim.routes.control
  (:require [compojure.core :refer :all]
            [clojure.test :refer [function?]]
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

(declare get-control-html)

(defn control []
  (layout/common 
      :include-js "js/site.js"
      [:br]
      [:div#info-line]
      [:div#textbox [:div#display.scroll-pane (get-control-html)]]

      [:div#controls        
        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "add", :class "server"} "="]]
          [:li [:a {:role "button", :href "#", :id "sub", :class "server"} "-"]]
        ]

        [:span.vert-split]

        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "play"} "1"]]
        ]
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


(defn table-x [ tableId recList options & args ]
  (let
    [radio      (get options :radio false)
     colData    (partition 3 args)
     colNames   (map first colData)
     colWidths  (map second colData)
     colFuncs   (map #(second (rest %)) colData)]
  
    [:table.table-x { :id tableId }
      [:colgroup
        (for [x colWidths]
          [:col {:width x}])
      ]
      [:tr
        (for [n colNames]
          [:th n])
      ]
      (map
        (fn [rec]
          (let [rowId (name (gensym "ID"))]
            [:tr 
              {:data-name (:setName rec)}
              (map 
                (fn [cn cw cf]
                  (if (= radio cn)
                    [:td
                      [:input {:type "radio" :name (name tableId) :id rowId :data-id (cf rec)}]]
                    ;; else
                    [:td
                      ( if radio
                        [:label {:for rowId} (cf rec)]
                        (cf rec))
                    ]
                  )
                )
                colNames
                colWidths
                colFuncs
              )
            ]
          )
        )
        recList
      )
    ]
  )
)


(defn get-control-html []
  (html5
    [:div.flexContainer
      [:div.flexItem
        (table-x :dataset (vals data-sets) {:radio ""} 
          "" "40px" :setName
          "Data Set" "100px" :setName 
          "Size" "50px" #(count (:fileList %)))
      ]
      [:div.flexItem
        (table-x :activity '("Review" "Cloze") {:radio ""}
          "" "40px" #(st/lower-case %) 
          "Session Type" "100px" (fn [x] x))
      ]
    ]
  )
)


;; Client event handlers

(defn gen-cmd-resp []
  (let [st (session/get :state)]
    [
    ]
  )
)


(defn event-startup []
  (if (empty? (session/get :state))
    (session/put! :state data-sets))

  ;; Return commands to client
  []
)


(defn event-add []
  (let
    [st (session/get :state)]
  []
  )
)


(defn event-sub []
  (let
    [st (session/get :state)]
  []
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
