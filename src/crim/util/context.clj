(ns crim.util.context
	(:require 
   		   [clojure.string :as st]
           [noir.session :as ns]))


(defmacro context+ [ & args ]
  `(let
     [~'+user  	(ns/get :user)
      ~'+state 	(ns/get :state)
      ~'+active	(ns/get :active)]
     ~@args))


(defmacro get+ [ks#]
  `(ns/get-in 
     (quote ~(map keyword (st/split (name ks#) #":")))))

(defmacro set+ [ks# v#]
  `(ns/assoc-in! (quote ~(map keyword (st/split (name ks#) #":"))) ~v#))

(defmacro clear+ []
  `(ns/clear!))

(defmacro let+ [varlist & forms]
  `(let
       ~((fn [vars]
          (loop [tail vars
                 res  []]
            (if (empty? tail)
              res
              ;; else
              (let
                [[sym val & rest] tail
                 new (into res [sym (if (keyword? val) `(get+ ~val) val)])]
                (recur rest new)))))
          varlist)
       ~@forms))