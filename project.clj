(defproject crim "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [ring-server "0.3.1"]
                 [org.clojure/data.json "0.2.6"]
                 ;;JDBC dependencies
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.xerial/sqlite-jdbc "3.8.11"]
                 ;;
                 [lib-noir "0.9.9" :exclusions [org.eclipse.jetty/jetty-io org.eclipse.jetty/jetty-util]]
                 ;; Audio file mp3 tag editing
                 [claudio "0.1.3"]
                 ]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler crim.handler/app
         :init crim.handler/init
         :destroy crim.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:resource-paths ["resources"]
    :dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.3.1"]]}})
