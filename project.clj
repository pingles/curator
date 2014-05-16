(defproject curator "0.0.1"
  :description "Clojurified Apache Curator"
  :url "https://github.com/pingles/curator"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.curator/curator-recipes "2.4.2"]
                 [org.apache.curator/curator-framework "2.4.2"]
                 [org.apache.curator/curator-x-discovery "2.4.2"]]
  :profiles {:dev {:dependencies [[org.slf4j/log4j-over-slf4j "1.6.4"]
                                  [org.slf4j/slf4j-simple "1.6.4"]]
                   :exclusions [org.slf4j/slf4j-log4j12]}}
  :scm {:name "git"
        :url "https://github.com/pingles/curator"})
