(ns crim.handler
  (:use compojure.core
        ring.middleware.resource
        ring.middleware.file-info
        hiccup.middleware
        crim.routes.home
        crim.routes.auth
        crim.routes.control)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [crim.models.db :as db]
            [noir.session :as session]
            [noir.validation :refer [wrap-noir-validation]]
            [ring.middleware.session.memory :refer [memory-store]]))

(defn init []
  (println "crim is starting")
  (if-not (.exists (java.io.File. "./db.sq3"))
      (db/create-tables)))

(defn destroy []
  (println "crim is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found Yo"))

(def app
  (-> (routes home-routes control-routes auth-routes app-routes)
      (handler/site)
      (wrap-base-url)
      (session/wrap-noir-session {:store (memory-store)})
      (wrap-noir-validation)
   )
)
