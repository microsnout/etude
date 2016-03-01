(ns crim.models.userdb
  (:require 
      [clojure.java.jdbc :as jdbc]
      [clojure.data.json :as json])
  (:import java.sql.DriverManager))


(def userdb-base {:classname     "org.sqlite.JDBC",
                  :subprotocol   "sqlite"
})

(defmacro with-user [name & forms]
  `(let [~'UserDB ~(assoc userdb-base :subname (str name ".sq3"))] ~@forms))

(defn create-tables* [db]
  (jdbc/db-do-commands
    db

    (jdbc/create-table-ddl
      :wordT
      [:word "TEXT PRIMARY KEY ON CONFLICT IGNORE"]
      [:score "INTEGER"]
    )

    (jdbc/create-table-ddl
      :refT
      [:wordid "INTEGER"]
      [:path   "TEXT"]
      ["UNIQUE" "(wordid, path) ON CONFLICT IGNORE"]
    )
  )
)

(defmacro create-tables []
  `(create-tables* ~'UserDB))

(defn add-word* [db wordStr]
  (get (first (jdbc/insert! db :wordT {:word wordStr, :score 0}))
       (keyword "last_insert_rowid()")))

(defmacro add-word [wordStr]
  `(add-word* ~'UserDB ~wordStr))

(defn get-words* [db]
  (jdbc/query db 
              ["SELECT * FROM wordT ORDER BY word ASC"] 
              :row-fn :word))

(defmacro get-words []
  `(get-words* ~'UserDB))

(defn get-word* [db wordStr]
  (jdbc/query db
              ["select rowid,word,score from wordT where word= ?" wordStr]
              :result-set-fn first))

(defmacro get-word [wordStr]
  `(get-word* ~'UserDB ~wordStr))

(defn get-wordid* [db wordStr]
  (jdbc/query db 
              ["select rowid from wordT where word = ?" wordStr] 
              :row-fn :rowid 
              :result-set-fn first))

(defmacro get-wordid [wordStr]
  `(get-wordid* ~'UserDB ~wordStr))

(defn update-word* [db wordStr newScore]
  (let [result (jdbc/execute! db 
                              ["update wordT set score = score + ? where word = ?" newScore wordStr])]
    (if (zero? (first result)) 
      (jdbc/insert! db :wordT {:word wordStr, :score newScore})
      result)))

(defmacro update-word [wordStr newScore]
  `(update-word* ~'UserDB ~wordStr ~newScore))

(defn add-ref* [db word pathStr]
  (let [id  (get-wordid* db word)
        idx (if (nil? id) (add-word* db word) id) 
        res (jdbc/insert! db :refT {:wordid idx, :path pathStr})]
    (get (first res) (keyword "last_insert_rowid()"))
  )
)

(defmacro add-ref [word pathStr]
  `(add-ref* ~'UserDB ~word ~pathStr))

(defn get-refs* [db wordStr]
  (if-let [id (get-wordid* db wordStr)]
    (jdbc/query db
                ["select path from refT where wordid = ?" id]
                :row-fn :path)
    ;; else
    ()))

(defmacro get-refs [wordStr]
  `(get-refs* ~'UserDB ~wordStr))







