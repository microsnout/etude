(ns crim.routes.auth
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [crim.models.db :as db]
            [crim.models.userdb :as udb]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.validation :refer [rule errors? has-value? on-error]]))


(defn error-item [[error]]
  [:div.error error])


(defn lf-control [fn-field key text]
  (list
    (label key text) [:br]
    (on-error key error-item)
    (fn-field key) [:br]
    ))

(defn show-registration-form []
    (form-to {:id "regbox", :class "formbox"} [:post "/register"]
      (lf-control text-field :userid "User Id:")
      (lf-control password-field :pass1 "Password:")
      (lf-control password-field :pass2 "Confirm Password:")
      (submit-button "Create New User")))


(defn show-login-form []
    (form-to {:id "logbox", :class "formbox"} [:post "/login"]
      (lf-control text-field :id "User Id:")
      (lf-control password-field :pass "Password:")
      (submit-button "Login")))


(defn registration-page []
  (layout/common
      [:h1 "Create New User"]
      [:div#homebox
        (show-registration-form)]
      [:br][:br]))


(defn login-page []
  (layout/common
      [:h1 "Login:"]
      [:div#homebox
        (show-login-form)]
      [:br][:br]))


(defn handle-login [id pass]
  (rule (has-value? id)
      [:id "screen name is required"])
  (rule (has-value? pass)
      [:pass "password is required"])

  (if (errors? :id :pass)
      (login-page)
      (let
        [user (db/get-user id)]
        (if (= pass (:pass user))
          (let
            [st (:state user {})
             cs (udb/with-user id (udb/get-current-session))]
            (session/put! :user id)
            (session/put! :state st)
            (session/put! :active cs)
            (redirect (if cs "/player" "/control")))
          (do
            (rule false [:pass "Invalid password"])
            (login-page))
        )
      )
  )
)


(defn handle-registration [userid pass1 pass2]
  (rule (has-value? userid)
      [:userid "User Id is required"])
  (rule (has-value? pass1)
      [:pass1 "Password is required"])
  (rule (= pass1 pass2)
      [:pass2 "Passwords must match"])

  (if (errors? :userid :pass1 :pass2)
      (registration-page)
      (try
        ;; Create new user with empty state
        (db/create-user userid pass1)
        (session/put! :user userid)
        (session/put! :state {})
        ;; Create users database 
        (println (str "create tables: " userid ))
        (udb/with-user userid
          (udb/create-tables))
        (redirect "/control")
        (catch Exception ex
          (println (str ex))
          (rule false [:userid "User Id aleady exists"])
          (registration-page))))
)


(defn handle-logout []
  (if-let [id (session/get :user)]
    (do
      ;; Save current user state in db and clear session
      (db/update-user-state id (session/get :state))
      (session/clear!)
      (redirect "/"))
    (println "LOGOUT no user")
  )
)


(defroutes auth-routes
  (GET "/register" [_] (registration-page))
  (POST "/register" [userid pass1 pass2]
        (handle-registration userid pass1 pass2))
  (GET "/login" [] (login-page))
  (POST "/login" [id pass]
        (handle-login id pass))
  (GET "/logout" []
       (handle-logout))
)
