(ns crim.models.db
  (:require 
      [clojure.java.jdbc :as sql]
      [clojure.data.json :as json])
  (:import java.sql.DriverManager))


(def db {:classname     "org.sqlite.JDBC",
         :subprotocol   "sqlite",
         :subname       "db.sq3"
})


(defn create-tables []
  (sql/with-connection
    db

    (sql/create-table
      :guestbook
      [:id "INTEGER PRIMARY KEY AUTOINCREMENT"]
      [:timestamp "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"]
      [:name "TEXT"]
      [:message "TEXT"])
    (sql/do-commands "CREATE INDEX timestamp_index ON guestbook (timestamp)")

    (sql/create-table
      :usertable
      [:userid "TEXT PRIMARY KEY"]
      [:pass "TEXT"]
      [:state "TEXT"])
  )
)

(defn read-guests []
  (sql/with-connection
    db
    (sql/with-query-results res
      ["SELECT * FROM guestbook ORDER BY timestamp DESC"]
      (doall res))))

(defn read-users []
  (sql/with-connection
    db
    (sql/with-query-results res
      ["SELECT * FROM usertable ORDER BY userid DESC"]
      (doall res))))


(defn get-user [userid]
  (sql/with-connection
    db
    (sql/with-query-results res
      ["select * from usertable where userid = ?" userid]
      (if-let [st (first res)]
        (assoc st :state (json/read-str (:state st) :key_fn keyword))
        {}
      )
    )
  )
)


(defn save-message [name message]
  (sql/with-connection
    db
    (sql/insert-values
      :guestbook
      [:name :message :timestamp]
      [name message (new java.util.Date)])))


(defn create-user [userid password]
  (sql/with-connection
    db
    (sql/insert-values 
      :usertable 
      [:userid :pass :state]
      [userid password (json/write-str {})] )))


(defn update-user-state [userid new-state]
  (sql/with-connection
    db
    (sql/update-values
      :usertable
      ["userid=?" userid]
      {:state (json/write-str new-state)}
    )
  )
)
