(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require [chameleon.config :refer [config]]
            [chameleon.handler :refer [handler]]
            [integrant.core :as ig]
            [chameleon.logging :as log]
            [chameleon.specs :as sp]
            [integrant.repl :refer [clear go halt init reset reset-all]]
            [integrant.repl.state :refer [system]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all disable-reload!]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.test :refer [run-tests run-all-tests]]
            [clojure.pprint :refer [pprint]]
            [chameleon.kafka :refer [deser]]))

(disable-reload! (find-ns 'integrant.core))




(def kafka-config {"client.id" "chameleon.developer"
                   "bootstrap.servers" "host:port"
                   "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
                   "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
                   "enable.auto.commit" "false"
                   "auto.offset.reset" "earliest"})

(integrant.repl/set-prep! (constantly (config {:event-config {:aai {:host "localhost:3904"
                                                                    :topic "events"
                                                                    :motsid ""
                                                                    :pass ""
                                                                    :consumer-group"chameleon1"
                                                                    :consumer-id"chameleon1"
                                                                    :timeout 15000
                                                                    :batch-size 8
                                                                    :type "HTTPAUTH"
                                                                    :kafka-config kafka-config}
                                                              :source :kafka}
                                               :gallifrey-host "localhost:443"
                                               :http-port 3449})))
