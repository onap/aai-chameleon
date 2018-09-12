(ns chameleon.testing
  (:require  [clojure.test :as t]
             [clojure.spec.alpha :as s]
             [clojure.spec.gen.alpha :as gen]
             [clojure.spec.test.alpha :as st]
             [chameleon.logging :as log]
             [chameleon.specs :as cs]
             [chameleon.route :as cr]
             [chameleon.aai-processor :as cai]
             [chameleon.config :as cc]
             [integrant.core :as ic]
             [integrant.core :as ig]
             [chameleon.aai-processor]))

;; STUBS
(s/fdef chameleon.route/assert-gallifrey!
  :args (s/cat :host :chameleon.specs/host
               :provenance :chameleon.specs/provenance
               :type :chameleon.specs/type
               :payload :chameleon.specs/payload
               :loggers :chameleon.specs/loggers)
  :ret nil?)

(s/fdef org.httpkit.client/request
  :args (s/cat :host :chameleon.specs/host
               :provenance :chameleon.specs/provenance
               :type :chameleon.specs/type
               :payload :chameleon.specs/payload
               :loggers :chameleon.specs/loggers)
  :ret (s/or :get :chameleon.specs/string
             :create (s/spec clojure.string/blank?  :gen #(gen/return ""))
             :update :chameleon.specs/string))

(s/fdef ig/ref
  :args (binding [s/*recursion-limit* 30]
          (s/cat :keyword (s/spec keyword? :gen-max 4)))
  :ret any?)

(s/fdef ig/load-namespaces
  :args (binding [s/*recursion-limit* 30]
          (s/cat :config (s/spec map? :gen-max 4)))
  :ret map?)


(s/fdef org.httpkit.client/request
  :args (s/cat :request map?)
  :ret :route/response)


;; TESTS
;; This requires a fix. I should return a random generated hashmap and not an empty one.
;; If i leave it at `(s/spec map?)` it takes about 7 minutes
;; to run the tests, so returning an empty hash-map for now.
(s/fdef chameleon.config/config
  :args (s/cat :config (s/spec map? :gen #(gen/return {})))
  :ret map?)

(s/fdef chameleon.aai-processor/from-spike
  :args (s/cat :gallifrey-host :chameleon.specs/host
               :payload :spike/payload
               :loggers :chameleon.specs/loggers)
  :ret nil?)

(s/fdef chameleon.aai-processor/from-gallifrey
  :args (s/cat :body :gallifrey/payload)
  :ret map?)

(s/fdef chameleon.aai-processor/gen-trim-relationship
  :args (s/cat :relationship :chameleon.specs/relationship)
  :ret map?)

(s/fdef chameleon.kafka/clj-kafka-consumer
  :args (s/cat :config :kafka/config :group-id :chameleon.specs/string
               :topic :chameleon.specs/string :logger :chameleon.specs/logger)
  :ret :kafka/consumer)

(s/fdef chameleon.kafka/consumer-record->clojure
  :args (s/cat :consumer-record :kafka/consumer-record)
  :ret :kafka/clojure-consumer-record)

(s/fdef chameleon.route/query
  :args (s/cat :host :chameleon.specs/host :key :kafka/key :type :chameleon.specs/type
               :gallifrey-params :gallifrey/payload)
  :ret string?)

(s/fdef chameleon.route/assert-create!
  :args (s/cat :host :chameleon.specs/host :actor :chameleon.specs/string
               :type :chameleon.specs/type :key :kafka/key
               :payload :gallifrey/payload)
  :ret string?)

(s/fdef chameleon.route/assert-update!
  :args (s/cat :host :chameleon.specs/host :actor :chameleon.specs/string
               :type :chameleon.specs/type :key :kafka/key
               :payload :gallifrey/payload)
  :ret string?)

(s/fdef chameleon.route/assert-delete!
  :args (s/cat :host :chameleon.specs/host :actor :chameleon.specs/string
               :type :chameleon.specs/type :key :kafka/key
               :payload :gallifrey/payload)
  :ret string?)


;; INSTRUMENTS - MOCKS
(st/instrument 'chameleon.route/assert-gallifrey! {:stub '(chameleon.route/assert-gallifrey!)})
(st/instrument 'org.httpkit.client/request {:stub '(org.httpkit.client/request)})
(st/instrument 'ig/ref {:stub '(ig/ref)})
(st/instrument 'ig/load-namespaces {:stub '(ig/load-namespaces)})

;; TESTING INSTRUMENTATIONS
#_(chameleon.route/assert-gallifrey! "host" "aai" "type" {}
                                     (log/error-logger "chameleon.testing") (log/audit-logger "chameleon.testing"))
#_(org.httpkit.client/request {"host" "aai"})
#_(integrant.core/load-namespaces {:foo "bar"})
#_(integrant.core/ref :foo)


(->> '(chameleon.config/config
       chameleon.aai-processor/from-spike
       chameleon.aai-processor/from-gallifrey
       chameleon.aai-processor/gen-trim-relationship
       chameleon.kafka/clj-kafka-consumer
       chameleon.kafka/consumer-record->clojure
       chameleon.route/query
       chameleon.route/assert-create!
       chameleon.route/assert-update!)
     st/check
     st/summarize-results)
