(ns crim.routes.control
  (:require [compojure.core :refer :all]
            [clojure.test :refer [function?]]
            [crim.views.layout :as layout]
            [crim.models.db :as db]
            [crim.models.userdb :as udb]
            [hiccup.page :refer [html5]]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [noir.response :refer [redirect]]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [crim.util.context :refer :all]
            ))

(declare get-control-html)
(declare get-words-html)

(defn get-control-buttons [] 
  (html5
        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "add", :class "server"} "="]]
          [:li [:a {:role "button", :href "#", :id "sub", :class "server"} "-"]]
        ]

        [:span.vert-split]

        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "play", :class "server"} "1"]]
        ]
  )
)


(defn get-words-buttons [] 
  (html5
        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "add", :class "server"} "="]]
          [:li [:a {:role "button", :href "#", :id "sub", :class "server"} "-"]]
        ]

        [:span.vert-split]

        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "play", :class "server"} "1"]]
        ]
  )
)

(defn control [content buttons]
  (layout/common 
      :include-js "js/site.js"
      [:br]
      [:div#info-line]
      [:div#textbox content]
      [:div#controls buttons]
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


(defn words []
)


;; Dataset Access funtions

(def blank-user-state
  {
    :activeSessions   []
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
  (let
    [data
      (for [ds (vals data-sets)
            ac ["Review" "Cloze"]]
        (assoc ds :activity ac))]
    (html5
      [:div#display.flexContainer
        [:div.flexItem
          (table-x :datasetT data {:radio ""} 
            "" "40px" #(str (:setName %) ":" (st/lower-case(:activity %)))
            "Data Set" "200px" #(str (:setName %) " - " (:activity %))
            "Size" "50px" #(count (:fileList %)))
        ]
      ]
    )
  )
)


(defn score-green [s]
  (bit-and s 0xFFFF))

(defn score-red [s]
  (bit-shift-right s 16))


(defn get-words-html []
  (let [id (get+ :user)]
    (html5
      [:div#display.scroll-pane
        (table-x :wordT 
                 (udb/with-user  id (udb/get-words))
                 {}
                 "Word" "120px" :word
                 "Percent" "60px" 
                    (fn [w]
                      (let [s (:score w)]
                        (str
                          (quot (* 100 (score-green s))
                                (+ (score-green s) (score-red s))) "%")))
                 "Good" "50px" #(score-green (:score %))
                 "Bad"  "50px" #(score-red (:score %))
        )
      ]
    )
  )
)

;; Client event handlers

(defn gen-cmd-resp []
  (let [st (get+ :state)]
    [
    ]
  )
)


(defn event-startup []
  (if (empty? (get+ :state))
    (set+ :state data-sets))

  ;; Return commands to client
  []
)


(defn event-add []
  (let
    [st (get+ :state)]
  []
  )
)


(defn event-sub []
  (let
    [st (get+ :state)]
  []
  )
)


(defn event-play [ args ]
  (let [user     (get+ :user)
        data     (st/split (:datasetT args) #":")
        setname  (keyword (first data))
        activity (keyword (second data))]
    (println "event-play:")
    (udb/with-user user
       (udb/create-new-session setname activity (setname data-sets))
       (set+ :active (udb/get-current-session)))
    [[:redirect "/player"]]
  ) 
)
;; ******


(defn post-user-event [ params ]
  (json/write-str
    (case (:id params)
      "startup" (event-startup)
      "add"     (event-add)
      "sub"     (event-sub)
      "play"    (event-play (:data params))
      [])
  )
)


(defn shutdown []
)


(defroutes control-routes
  (GET "/ctl-get-control-html" request (get-control-html))
  (POST "/ctl-post-user-event" request (post-user-event (:params request)))
  (GET "/control" [] (control (get-control-html) (get-control-buttons)))
  (GET "/userlist" [] (userlist))
  (GET "/words" [] (control (get-words-html) (get-words-buttons)))
  (GET "/logout" []
       (shutdown)
       nil)
)
