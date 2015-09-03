(defproject curator "1.0.0-SNAPSHOT"
  :description "Clojurified Apache Curator"
  :url "https://github.com/pingles/curator"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.curator/curator-recipes "2.8.0"]
                 [org.apache.curator/curator-framework "2.8.0"]
                 [org.apache.curator/curator-x-discovery "2.8.0"]
                 [org.clojure/java.data "0.1.1"]]
  :profiles {:dev {:dependencies [[org.slf4j/log4j-over-slf4j "1.7.12"]
                                  [org.slf4j/slf4j-simple "1.7.12"]]
                   :exclusions [org.slf4j/slf4j-log4j12]}}
  :scm {:name "git"
        :url "https://github.com/pingles/curator"})
