(defproject chameleon "0.1.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.7theta/utilis "1.0.4"]
                 [http-kit "2.2.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-anti-forgery "1.1.0"]
                 [compojure "1.6.0"]
                 [liberator "0.15.1"]
                 [cheshire "5.7.1"]
                 [inflections "0.13.0"]
                 [clj-time "0.14.2"]
                 [integrant "0.6.2"]
                 [yogthos/config "0.9"]
                 [org.onap.aai.event-client/event-client-dmaap "1.2.1"]
                 [org.onap.aai.logging-service/common-logging "1.2.2"]
                 [camel-snake-kebab "0.4.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [org.clojure/test.check "0.9.0"]
                 [cloverage/cloverage "1.0.10"]]
  :plugins [[lein-cloverage "1.0.10"]]
  :repositories [["onap-releases" {:url "https://nexus.onap.org/content/repositories/releases/"}]
                 ["onap-public" {:url "https://nexus.onap.org/content/repositories/public/"}]
                 ["onap-staging" {:url "https://nexus.onap.org/content/repositories/staging/"}]
                 ["onap-snapshot" {:url "https://nexus.onap.org/content/repositories/snapshots/"}]]
  :min-lein-version "2.5.3"
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[ring/ring-devel "1.6.3"]
                                  [integrant/repl "0.2.0"]]}
             :uberjar {:source-paths ["prod"]
                       :main chameleon.server
                       :aot [chameleon.server]
                       :uberjar-name "chameleon.jar"}}
  :prep-tasks ["compile"]
  :source-paths ["src" "prod" "test"]
  :resource-paths ["resources"]
  :pom-plugins [[com.theoryinpractise/clojure-maven-plugin "1.3.13"
                 {:extensions "true"
                  :configuration [:sourceDirectories
                                  [:sourceDirectory "src"]
                                  [:sourceDirectory "prod"]
                                  [:sourceDirectory "test"]]
                  :executions ([:execution [:id "compile"]
                                [:goals ([:goal "compile"])]
                                [:phase "compile"]])}]
                [org.apache.maven.plugins/maven-jar-plugin "2.4"
                 {:configuration [:archive [:manifest
                                            [:addClasspath true]
                                            [:mainClass "chameleon.server"]
                                            [:classpathPrefix "dependency"]]]}]
                [org.apache.maven.plugins/maven-dependency-plugin "2.8"
                 {:executions ([:execution [:id "copy-dependencies"]
                                [:goals ([:goal "copy-dependencies"])]
                                [:phase "package"]])}]
                [org.apache.maven.plugins/maven-assembly-plugin "2.4.1"
                 {:configuration ([:descriptorRefs [:descriptorRef "jar-with-dependencies"]]
                                  [:archive [:manifest
                                             [:mainClass "chameleon.server"]]])
                  :executions ([:execution [:id "assemble"]
                                [:phase "package"]
                                [:goals ([:goal "single"])]])}]])
