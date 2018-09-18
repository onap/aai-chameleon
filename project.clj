(defproject org.onap.aai/chameleon "1.3.0-SNAPSHOT"
  :name "chameleon"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
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
                 [org.onap.aai.event-client/event-client-dmaap "1.3.0"]
                 [org.onap.aai.event-client/event-client-kafka "1.3.0"]
                 [org.onap.aai.logging-service/common-logging "1.2.2"]
                 [camel-snake-kebab "0.4.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [org.clojure/test.check "0.9.0"]
                 [cloverage/cloverage "1.0.10"]]
  :plugins [[lein-cloverage "1.0.10"]]
  :min-lein-version "2.5.3"
  :repositories [["ecomp-snapshots" {:name "ECOMP Snapshot Repository" :url "https://nexus.onap.org/content/repositories/snapshots/"}]
                 ["onap-releases" {:url "https://nexus.onap.org/content/repositories/releases/"}]]
  :pom-addition [:distributionManagement
                 [:repository
                  [:id "ecomp-releases"]
                  [:name "ECOMP Release Repository"]
                  [:url "https://nexus.onap.org/content/repositories/releases/"]]
                 [:snapshotRepository
                  [:id "ecomp-snapshots"]
                  [:name "ECOMP Snapshot Repository"]
                  [:url "https://nexus.onap.org/content/repositories/snapshots/"]]]
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
                [org.apache.maven.plugins/maven-shade-plugin "3.2.0"
                 {:executions ([:execution
                                [:phase "package"]
                                [:goals ([:goal "shade"])]])}]
                [org.sonatype.plugins/nexus-staging-maven-plugin "1.6.7"
                 {:extensions true
                  :configuration ([:nexusUrl "https://nexus.onap.org"]
                                  [:stagingProfileId "176c31dfe190a"]
                                  [:serverId "ecomp-staging"])}]
                [com.spotify/dockerfile-maven-plugin "1.4.4"
                 {:configuration ([:tag "latest"]
                                  [:repository "${docker.push.registry}/onap/chameleon"]
                                  [:verbose true]
                                  [:serverId "docker-hub"])
                  :executions ([:execution [:id "default"]])}]])
