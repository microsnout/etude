(ns crim.routes.auth
  (:require [compojure.core :refer :all]
            [crim.views.layout :as layout]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [crim.models.db :as db]
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
          (do
            (session/put! :user id)
            (session/put! :state (:state user))
            (redirect "/control"))
          (do
            (rule false [:pass "Invalid password"])
            (login-page)))))
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
        (db/create-user userid pass1)
        (session/put! :user userid)
        (redirect "/control")
        (catch Exception ex
          (rule false [:userid "User Id aleady exists"])
          (registration-page))))
)


(defroutes auth-routes
  (GET "/register" [_] (registration-page))
  (POST "/register" [userid pass1 pass2]
        (handle-registration userid pass1 pass2))
  (GET "/login" [] (login-page))
  (POST "/login" [id pass]
        (handle-login id pass))
  (GET "/logout" []
       (session/clear!)
       (redirect "/"))

)
