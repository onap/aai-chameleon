(ns chameleon.specs
  (:require [clojure.spec.alpha :as s]
            [chameleon.logging :as log]
            [clojure.spec.gen.alpha :as gen]
            [cheshire.core :as c]
            [clojure.string :as str]))

(s/def ::host string?)
(s/def ::provenance string?)
(s/def ::payload map?)
(s/def ::string (s/spec (s/and string? (complement str/blank?)) :gen gen/string-alphanumeric))
(s/def ::type (s/spec ::string :gen #(gen/elements ["vserver" "pserver" "generic-vnf"])))
(s/def ::id uuid?)
(s/def ::source (s/keys :req-un [::type ::id]))
(s/def ::target (s/keys :req-un [::type ::id]))
(s/def ::_id ::id)
(s/def ::spec-form-pair (s/* (s/cat :spec #(% (s/registry)) :form any?)))

;; spike event
(s/def :spike/transaction-id uuid?)
(s/def :spike/schema-version (s/spec #(re-matches #"v\d+" %) :gen #(->> (s/int-in 7 15)
                                                                        s/gen
                                                                        (gen/fmap (partial str "v")))))
(s/def :spike/key uuid?)
(s/def :spike/name ::string)
(s/def :spike/last-mod-source-of-truth ::string)
(s/def :spike/source-of-truth ::string)
(s/def :spike/vserver-selflink ::string)
(s/def :spike/is-closed-loop-disabled boolean?)
(s/def :spike/in-maint boolean?)
(s/def :spike/timestamp inst?)
(s/def :spike/operation (s/spec ::string :gen #(gen/elements ["UPDATE" "CREATE" "DELETE"])))
(s/def :spike/properties (s/keys :req-un [:spike/name ::id :spike/last-mod-source-of-truth
                                          :spike/source-of-truth :spike/vserver-selflink
                                          :spike/is-closed-loop-disabled :spike/in-maint]))
(s/def :spike/vertex (s/keys :req-un [:spike/schema-version ::type :spike/key :spike/properties]))
(s/def :spike/event (s/keys :req-un [:spike/transaction-id :spike/vertex
                                     :spike/operation :spike/timestamp]))
(s/def :spike/payload (s/spec string? :gen #(gen/fmap (partial c/generate-string)
                                                      (s/gen :spike/event))))
(s/def :gallifrey/k-end-actor ::string)

;; gallifrey response
(s/def :gallifrey/k-start-actor ::string)
(s/def :gallifrey/k-end inst?)
(s/def :gallifrey/k-start inst?)
(s/def :gallifrey/history (s/keys :req-un [:gallifrey/k-end-actor :gallifrey/k-end
                                           :gallifrey/k-start-actor :gallifrey/k-start]))
(s/def :relationship/_meta (s/map-of ::string :gallifrey/history))
(s/def :relationship/_type (s/spec string? :gen #(gen/return "relationship")))
(s/def :gallifrey/_type (s/spec ::string :gen #(gen/return "entity")))
(s/def :gallifrey/properties (s/keys :req-un [:gallifrey/_type ::type]))
(s/def :relationship/type (s/spec string? :gen #(->> (gen/string-alphanumeric)
                                                     (gen/such-that (complement str/blank?))
                                                     (gen/fmap (partial str "tosca.relationship.")))))
(s/def :relationship/properties (s/keys :req-un [:relationship/_type :relationship/type]))
(s/def ::relationship (s/keys :req-un [:relationship/properties ::source
                                       ::target :relationship/_meta ::_id]))
(s/def ::relationships (s/coll-of ::relationship :gen-max 8))
(s/def :gallifrey/payload (s/spec map?
                                  :gen #(->> [::_id :gallifrey/properties
                                              :gallifrey/properties ::relationships]
                                             (s/keys :req-un)
                                             s/gen
                                             (gen/fmap (partial clojure.walk/stringify-keys)))))

;; Logger specs
(s/def ::logger (s/spec log/logger? :gen #(gen/return (log/error-logger "chameleon.specs"))))
(s/def ::loggers (s/cat :e :chameleon.specs/logger :a :chameleon.specs/logger))
(s/def :logging/msgs (s/* string?))
(s/def :logging/valid-fields log/valid-logfields?)
