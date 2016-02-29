(ns crim.models.db
  (:require 
      [clojure.java.jdbc :as jdbc]
      [clojure.data.json :as json])
  (:import java.sql.DriverManager))


(def db {:classname     "org.sqlite.JDBC",
         :subprotocol   "sqlite",
         :subname       "db.sq3"
})


(defn create-tables []
  (jdbc/db-do-commands
    db

    (jdbc/create-table-ddl
      :guestbook
      [:id "INTEGER PRIMARY KEY AUTOINCREMENT"]
      [:timestamp "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"]
      [:name "TEXT"]
      [:message "TEXT"])

    (jdbc/create-table-ddl
      :usertable
      [:userid "TEXT PRIMARY KEY"]
      [:pass "TEXT"]
      [:state "TEXT"])
  )
)

(defn read-guests []
  (jdbc/query db
    ["SELECT * FROM guestbook ORDER BY timestamp DESC"] ))

(defn read-users []
  (jdbc/query db
    ["SELECT * FROM usertable ORDER BY userid DESC"] ))


(defn get-user [userid]
  (let 
    [usr (jdbc/query db 
                     ["select * from usertable where userid = ?" userid] 
                     :result-set-fn first)]
    (if (:state usr) 
      (assoc usr :state (json/read-str (:state usr) :key-fn keyword))
      {}
    )
  )
)


(defn save-message [name message]
  (jdbc/insert!
    db
    :guestbook
    {:name name :message message :timestamp (new java.util.Date)}
  )
)


(defn create-user [userid password]
  (jdbc/insert!
    db
    :usertable 
    {:userid userid :pass password :state (json/write-str {})}
  )
)


(defn update-user-state [userid new-state]
  (jdbc/update!
    db
    :usertable
    {:state (json/write-str new-state)}
    ["userid=?" userid]
  )
)
