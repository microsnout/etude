(ns crim.models.userdb
  (:require 
      [clojure.java.jdbc :as jdbc]
      [clojure.data.json :as json])
  (:import java.sql.DriverManager))


(def-schema

  {:dbx-defs
   	{
     :site-db
		{
		 :dbx-spec
	  		{ :classname	"org.sqlite.JDBC",
	     	  :subprotocol	"sqlite",
	          :subname		"db.sq3" }
	    
	     :dbx-tables
      		{
		   	 :userT
		     	[
		       		[:user  :text :primary]
		         	[:pass  :text]
		          	[:state :json]
		        ] 
		      
		     :guestbookT
		       	[
		       		[:date    :timestamp]
		         	[:user    :text
		          	[:message :text]
		        ]
         	}
	  	} 
 
 	 :user-db
	 	{
	   	 :dbx-spec
	  		{ :classname	"org.sqlite.JDBC",
	     	  :subprotocol	"sqlite",
	          :subname		"userdb.sq3", 
	          :filepath		["users" :site-db:user] }

	     :dbx-tables
            { 
	         :wordT
		        [
		         	[:word :text :primary :conflict-ignore]
		          	[:score :integer]
		        ]
		        
		     :refT
		        [
		         	[:word [:wordT :word]]
		          	[:path :text]
		            ["UNIQUE" "(word, path) ON CONFLICT IGNORE"]
		        ]
		        
		     :sessionT
		        [
		         	[:rowid :integer]
		          	[:start :timestamp]
		            [:pindex :integer]
		            [:mingap :integer]
		            [:setname :text]
		            [:activity :keyword]
		            [:dataset :json]
		        ]
	    	}
	    }
  	}
  }
)


(def row-attrs 
  { :text "TEXT" :integer "INTEGER" :timestamp "TIMESTAMP" :json "TEXT" 
    :primary "PRIMARY KEY" :conflict-ignore "ON CONFLICT IGNORE" })

(def *schema* (atom {}))


(defn def-schema [sch]
	(swap! *schema* (fn [x y] y) sch)
)


(defn create-db-schema [db-key]
  (let [db-def  (get-in @*schema* [:dbx-defs db-key])
        db-spec (:dbx-spec db-def)]
    (try
	    (apply
	      jdbc/db-do-commands
	      (cons 
		    db-spec
		    (map 
		      ;; For each table definition
		      (fn [[table-key table-def]] 
			    (apply 
			      jdbc/create-table-ddl
			      (cons table-key
			            (map 
                 		  ;; For each row definition
			              (fn [row-spec]
	                  		(loop [rowname (first row-spec)
	                           	   strspec ""
	                               specs   (rest row-spec)]
	                      		(if (empty? specs)
	                          	  [rowname str]
	                              (recur rowname 
	                                     (str strspec (get row-attrs (first specs) (first specs)) " ") 
	                                     (rest specs) ))))
			              table-def))))
			  (:dbx-tables db-def))
		  )
		)
		(catch Exception ex
    	  (print-sql-exception ex))
  	)
  )
)


(dbx/get :user-db:wordT)














