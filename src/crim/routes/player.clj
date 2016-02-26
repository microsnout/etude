(ns crim.routes.player
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [noir.session :as session]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [claudio.id3 :as id3]
            ))


(defn player [req]
  (session/put! :active (keyword (get req :dataset)))
  (session/put! :activity (keyword (get req :activity)))

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


(defn get-active-session []
  (let
    [user-state (session/get :state)
     active-name (session/get :active)]
    (get user-state active-name)))


(defn update-active-state [key val]
  (let
    [user-state (session/get :state)
     active-name (session/get :active)
     active (get user-state active-name)]
    (session/put!
      :state
      (assoc 
        user-state
        active-name
        (assoc active key val)))
  )
)


;; Client event handlers

(defn gen-cmd-resp []
  (let [st (get-active-session)
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

  (gen-cmd-resp)
)


(defn event-next []
  (let
    [st (get-active-session)
     px (:playIndex st)
     nx (mod (inc px) (count (:fileList st)))]
    (update-active-state :playIndex nx)
    (gen-cmd-resp)
  )
)


(defn event-back []
  (let
    [st (get-active-session)
     px (:playIndex st)
     nx (mod (dec px) (count (:fileList st)))]
    (update-active-state :playIndex nx)
    (gen-cmd-resp)
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
  (let [activity (session/get :activity)]
    (if (= activity :review)
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
      "ended"   (event-next)
      [])
  )
)


(defn shutdown []
)


(defroutes player-routes
  (GET "/play-get-text" request (play-get-text (:params request)))
  (POST "/play-post-user-event" request (play-post-user-event (:params request)))
  (GET "/player" request (player (:params request)))
;;  (GET "/logout" []
;;       (shutdown)
;;       nil)
)
