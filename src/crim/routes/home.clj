(ns crim.routes.home
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [crim.routes.auth :as auth]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [crim.models.db :as db]
            [noir.response :refer [redirect]]
            [noir.session :as session]))

(defn format-time [timestamp]
  (-> "dd/MM/yyyy"
      (java.text.SimpleDateFormat.)
      (.format timestamp)))


(defn guestbook [& [message error]]
 (layout/common
   [:h1 "Guestbook"]

   [:ul.guests
      (for [{:keys [message name timestamp]} (db/read-guests)]
        [:li.guests
          [:p.guests \" message \"]
          [:p.guests "- " [:cite.guests name]]
          [:time.guests (format-time timestamp)]])]

   (if-let [id (session/get :user)]
      (form-to [:post "/guestbook"]
        [:hr][:br]
        (label :message "Comment:")
        [:br][:span.error error][:br]
        (text-area {:rows 6 :cols 50} "message" message)
        [:br]
        (submit-button "Submit")))

    [:br]))


(defn home []
  (if (session/get :user)
    (redirect "/control")
    (layout/common
        [:div#homebox
          (auth/show-login-form)
          [:div#homeboxGap ""]
          (auth/show-registration-form)]
        [:br]
    )
  )
)


(defn save-message [message]
 (let [name (session/get :user)]
   (cond
     (empty? name)
     (guestbook message "Error: No user id")
     (empty? message)
     (guestbook message "Don't you have something to say?")
     :else
     (do
       (db/save-message name message)
       (guestbook)))))


(defroutes home-routes
 (GET "/" [] (home))
 (GET "/guestbook" [] (guestbook))
 (POST "/guestbook" [message] (save-message message))
 )
