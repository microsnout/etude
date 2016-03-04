(ns crim.routes.player
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.models.userdb :as udb]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [noir.session :as session]
            [noir.response :refer [redirect]]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [claudio.id3 :as id3]
            ))


(defn player [req]
  (layout/common 
      :include-js "js/player.js"
      [:br]
      [:div#info-line.flexContainer
        [:div#play-title] [:div#play-score]]
      [:div#textbox [:div#display.scroll-pane]]

      [:div#controls        
        [:ul.ctl-list
          [:li [:a {:role "button", :href "#", :id "stop", :class "server"} "'"]]
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



;; Client event handlers

(defn gen-cmd-resp []
  (let [ac (session/get :active)
        sn (:setname ac)
        px (:pindex ac)
        ds (:dataset ac)
        fn ((:fileList ds) px)
        tp (str (:textPath ds) fn (:textExt ds))
        ap (str (:audioPath ds) fn (:audioExt ds))
        io (clojure.java.io/file (str "resources/public/" ap))
        il (str sn ": " (if (:id3Title ds) (:title (id3/read-tag io)) fn))]
    [
      [:loadText tp]
      [:loadAudio ap]
      [:setInfoLine il]
    ]
  )
)


(defn update-pindex[px]
  (session/assoc-in! (list :active :pindex) px))


(defn event-startup []
  ;; Turn off debugging output from claudio
  (.setLevel (java.util.logging.Logger/getLogger "org.jaudiotagger")
           java.util.logging.Level/OFF)

  (gen-cmd-resp)
)


(defn event-next []
  (let
    [user (session/get :user)
     ac (session/get :active)
     px (:pindex ac)
     nx (mod (inc px) (count (:fileList (:dataset ac))))]
    (update-pindex nx)
    (udb/with-user user (udb/update-play-index nx))
    (gen-cmd-resp)
  )
)


(defn event-ended [ results ]
  (let
    [id (session/get :user)
     ac (session/get :active)
     px (:pindex ac)
     nx (mod (inc px) (count (:fileList (:dataset ac))))]

    (update-pindex nx)
    
    (udb/with-user id
      (map 
        (fn [[word score]] 
          (udb/update-word word score))
        (:words results)
      )
    )
    ;; Update user db here
    ;; Change code above to terminate play not restart
    
    (gen-cmd-resp)
  )
)


(defn event-back []
  (let
    [user (session/get :user)
     ac (session/get :active)
     px (:pindex ac)
     nx (mod (dec px) (count (:fileList (:dataset ac))))]
    (update-pindex nx)
    (udb/with-user user (udb/update-play-index nx))
    (gen-cmd-resp)
  )
)


(defn event-stop []
  (let [user (session/get :user)
        ac (session/get :active)]
    (println "event-stop:")
    (udb/with-user user 
      (udb/delete-session 1))
    (session/put! :active nil)
    [[:redirect "/control"]]
  )
)


;; ******


;; mmake-word-index
;;  - find all acceptable words in text
;;  - return ( (s e "word1") (s e "word2") ) list of start end index of words
;;
(defn make-word-index [txt]
  (seq
    (loop [match (re-matcher #"\p{L}[-'\p{L}]+\p{L}" txt)
           out []]
      (if (.find match)
        (recur
          match
          (conj out (list (.group match) (.start match) (.end match))) 
        )
        out
      )
    )
  )
)


(defn make-cloze-text [ txt word-list min-gap ]
  (apply str
    (loop [words  word-list
           frags  []
           lastx  0
           gap    (* 5 (quot (rand-int min-gap) 5))]
      (if (empty? words)
        ;; Return list of string fragments - adding tailing frag
        (conj 
          frags 
          (.substring txt lastx (count txt)))
        ;; else there are more words
        (let [[wstr startx endx]  (first words)]
          (if (>= startx (+ lastx gap))
            ;; Replace this word with input box
            (recur
              (rest words)
              (conj 
                frags 
                (.substring txt lastx startx) 
                (format 
                    "<input type='text' class='cloze' spellcheck=false font-weight=bold size=%d data-word='%s'>" 
                    (count wstr) (.toLowerCase wstr)))
              endx
              min-gap
            )
            ;; else skip this word
            (recur
              (rest words) frags lastx gap)
          )
        )
      )
    )
  )
)


(defn play-get-text [req]
  (let [ac (session/get :active)]
    (if (= (:activity ac) :review)
      ;; simple text review
      (slurp (:url req))
      ;; else cloze test
      (let [txt (slurp (:url req))]
        (make-cloze-text 
          txt
          (make-word-index txt)
          80)
      )
    )
  )
)


(defn play-post-user-event [ params ]
  (json/write-str
    (case (:id params)
      "startup" (event-startup)
      "next"    (event-next)
      "back"    (event-back)
      "ended"   (event-ended (:data params))
      "stop"    (event-stop)
      [])
  )
)


(defn shutdown []
)


(defroutes player-routes
  (GET "/play-get-text" request (play-get-text (:params request)))
  (POST "/play-post-user-event" request (play-post-user-event (:params request)))
  (GET "/player" request (player (:params request)))
  (GET "/logout" []
       (shutdown)
       nil)
)
