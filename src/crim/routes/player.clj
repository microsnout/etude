(ns crim.routes.player
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.models.userdb :as udb]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [noir.response :refer [redirect]]
            [clojure.string :as st]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [claudio.id3 :as id3]
            [crim.util.context :refer :all]
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
          [:li [:a {:role "button", :href "#", :id "replay", :class "server"} "9"]]
          [:li [:a {:role "button", :href "#", :id "next", :class "server"} "8"]]
          [:li [:a {:role "button", :href "#", :id "loop", :class "server"} "("]]
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
  (let [ac (get+ :active)
        sn (:setname ac)
        px (:pindex ac)
        ds (:dataset ac)
        fn ((:fileList ds) px)
        tp (str (:textPath ds) fn (:textExt ds))
        ap (str (:audioPath ds) fn (:audioExt ds))
        io (clojure.java.io/file (str "resources/public/" ap))
        il (str sn ": " (if (:id3Title ds) (:title (id3/read-tag io)) fn))]

    (set+ :refpath (str sn ":" fn))

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

  (set+ :loopMode false)
  (gen-cmd-resp)
)


(defn event-next []
  (let+
    [user :user
     ds   :active:dataset
     px   :active:pindex
     nx   (mod (inc px) (count (:fileList ds)))]
    (set+ :active:pindex nx)
    (udb/with-user user (udb/update-play-index nx))
    (gen-cmd-resp)
  )
)


(defn event-ended [ results ]
  (println (str "event-ended: " results))
  (let+
    [id :user
     ds :active:dataset
     px :active:pindex
     rp :refpath
     nx (mod (inc px) (count (:fileList ds)))
     rs (json/read-str results :key-fn keyword)]

    (if (not (get+ :loopMode)) 
      (set+ :active:pindex nx))

    (udb/with-user id
      (doseq [[word score] (get rs :words)] 
          (udb/update-word word score)
          (udb/add-ref word rp))
    )
    ;; Change code above to terminate play not restart
    
    (gen-cmd-resp)
  )
)


(defn event-back []
  (let+
    [user :user
     ds   :active:dataset 
     px   :active:pindex
     nx   (mod (dec px) (count (:fileList ds)))]
    (set+ :active:pindex nx)
    (udb/with-user user (udb/update-play-index nx))
    (gen-cmd-resp)
  )
)


(defn event-stop []
  (let [user (get+ :user)
        ac   (get+ :active)]
    (udb/with-user user 
      (udb/delete-session 1))
    (set+ :active nil)
    [[:redirect "/control"]]
  )
)


(defn event-loop []
  (set+ :loopMode (not (get+ :loopMode)))
)


(defn event-replay []
  (gen-cmd-resp))


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
                    (+ 1 (count wstr)) (.toLowerCase wstr)))
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
  (let [ac (get+ :active)]
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
      "loop"    (event-loop)
      "replay"  (event-replay)
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
