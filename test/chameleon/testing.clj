(ns chameleon.testing
  (:require  [clojure.test :as t]
             [clojure.spec.alpha :as s]
             [clojure.spec.gen.alpha :as gen]
             [clojure.spec.test.alpha :as st]
             [chameleon.logging :as log]
             [chameleon.specs :as cs]
             [chameleon.route :as cr]
             [chameleon.aai-processor :as cai]))

(s/fdef chameleon.route/assert-gallifrey!
        :args (s/cat :host :chameleon.specs/host
                     :provenance :chameleon.specs/provenance
                     :type :chameleon.specs/type
                     :payload :chameleon.specs/payload
                     :loggers :chameleon.specs/loggers)
        :ret nil?)

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

(st/instrument 'chameleon.route/assert-gallifrey! {:stub '(chameleon.route/assert-gallifrey!)})

;; Testing instrumentation
(chameleon.route/assert-gallifrey! "host" "aai" "type" {} (log/error-logger "chameleon.testing")  (log/audit-logger "chameleon.testing"))

(->> '(chameleon.aai-processor/from-spike
       chameleon.aai-processor/from-gallifrey
       chameleon.aai-processor/gen-trim-relationship)
     st/check
     st/summarize-results)
