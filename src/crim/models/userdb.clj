(ns crim.models.userdb
  (:require 
      [clojure.java.jdbc :as jdbc]
      [clojure.data.json :as json])
  (:import java.sql.DriverManager))


(def userdb-base {:classname     "org.sqlite.JDBC",
                  :subprotocol   "sqlite"
})

(defmacro with-user [username & forms]
  `(let [~'UserDB (assoc ~userdb-base :subname (str ~username ".sq3"))] ~@forms))

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
      [:word   "TEXT"]
      [:path   "TEXT"]
      ["UNIQUE" "(word, path) ON CONFLICT IGNORE"]
    )

    (jdbc/create-table-ddl
      :sessionT
      [:rowid    "INTEGER"]
      [:start    "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"]
      [:pindex   "INTEGER DEFAULT 0"]
      [:mingap   "INTEGER DEFAULT 80"]
      [:setname  "TEXT"]
      [:activity "TEXT"]
      [:dataset  "TEXT"]
    )
  )
)

(defmacro create-tables []
  `(create-tables* ~'UserDB))

(defn update-session* [db values]
  (let 
    [result 
      (jdbc/update! 
        db
        :sessionT 
        values
        ["rowid = ?" 1]
      )
    ]
    (if (zero? (first result)) 
      (jdbc/insert! db :sessionT (assoc values :rowid 1))
      result
    )
  )
)

(defn create-new-session* [db setname activity dataset mingap]
  (update-session* 
    db 
    {:setname (name setname) :activity (name activity) :dataset (json/write-str dataset) :mingap mingap
     :start "CURRENT_TIMESTAMP" :pindex 0 }
  )
)

(defmacro create-new-session [setname activity dataset & {:keys [mingap] :or {mingap 80}}]
  `(create-new-session* ~'UserDB ~setname ~activity ~dataset ~mingap))

(defn delete-session* [db rowid]
  (jdbc/delete! db :sessionT ["rowid = ?" rowid]))

(defmacro delete-session [rowid]
  `(delete-session* ~'UserDB ~rowid))

(defn update-play-index* [db px]
  (update-session* db {:pindex px}))

(defmacro update-play-index [px]
  `(update-play-index* ~'UserDB ~px))

(defn get-current-session* [db]
  (let [sess (jdbc/query db 
                         ["select * from sessionT where rowid = ?" 1]
                         :result-set-fn first)]
    (if-let [ds (:dataset sess)]
      (assoc sess :dataset (json/read-str ds :key-fn keyword) :activity (keyword (:activity sess))))))

(defmacro get-current-session []
  `(get-current-session* ~'UserDB))

(defn add-word* [db wordStr]
  (get (first (jdbc/insert! db :wordT {:word wordStr, :score 0}))
       (keyword "last_insert_rowid()")))

(defmacro add-word [wordStr]
  `(add-word* ~'UserDB ~wordStr))

(defn get-words* [db]
  (jdbc/query db 
              ["SELECT * FROM wordT ORDER BY word ASC"] ))

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
  (let [res (jdbc/insert! db :refT {:word word, :path pathStr})]
    (get (first res) (keyword "last_insert_rowid()"))
  )
)

(defmacro add-ref [word pathStr]
  `(add-ref* ~'UserDB ~word ~pathStr))

(defn get-refs* [db word]
  (jdbc/query db
                ["select path from refT where word = ?" word]
                :row-fn :path))

(defmacro get-refs [wordStr]
  `(get-refs* ~'UserDB ~wordStr))







