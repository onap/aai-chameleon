(ns chameleon.aai-processor
  (:require [chameleon.txform :refer :all]
            [chameleon.route :refer :all]
            [cheshire.core :refer :all]
            [clojure.set :refer :all]))

(defn- gen-trim-relationship
  "Generates a trimmed down version of the relationship containing only the id, type, url, and target"
  [relationship]
  (let [id (relationship"_id")
        type (get-in relationship ["properties" "type"])
        src-id (get-in relationship ["source" "id"])
        src-type (get-in relationship ["source" "type"])
        target-id (get-in relationship ["target" "id"])
        target-type (get-in relationship ["target" "type"])]
    {"id" id
     "type" type
     "source" {"id" src-id "type" src-type}
     "target" {"id" target-id "type" target-type}})
  )

(defn from-gallifrey
  "Transforms Gallifrey response payloads into a format consumable by AAI-centric clients"
  [body]
  (let [resource-type (get-in body ["properties" "_type"])
        id (body "_id")
        type (get-in body ["properties" "type"])
        properties (body "properties")
        entity-response {
                         "id" id
                         "type" type
                         "properties" (dissoc properties "_type" "type")
                         }]
    (if (= resource-type "entity")
                                        ; Transform into an entity type
      (let [relationships (body "relationships")]
        (assoc entity-response
               "in" (into [] (map gen-trim-relationship (filter #(= (get-in % ["target" "id"]) id) relationships)))
               "out" (into [] (map gen-trim-relationship (filter #(= (get-in % ["source" "id"]) id) relationships)))))
      entity-response)))

(defn from-spike
  "Transforms Spike-based event payloads to a format accepted by Gallifrey for vertices and relationships"
  [gallifrey-host payload]
  (println payload)
  (let [txpayload (map-keywords (parse-string payload))
        operation (:operation txpayload)
        parse-type (if (contains? txpayload :vertex)
                     :vertex
                     :relationship)
        entity-type (if (contains? txpayload :vertex)
                      :entity
                      :relationship)
        entity (map-keywords (parse-type txpayload))
        key (:key entity)
        properties (assoc (:properties entity) :type (:type entity))
        truth-time (:truth-time entity)
        entity-assertion {:meta {:key key
                                 :operation operation
                                 :time {:t-t truth-time}}}]
    (assert-gallifrey gallifrey-host "aai" (name entity-type) (if (= entity-type :entity)
                                                                (assoc entity-assertion :body (generate-string {:properties properties}))
                                                                (assoc entity-assertion :body (generate-string (conj {:properties properties}
                                                                                                                     {:source  (rename-keys (:source entity) {"key" "id"})}
                                                                                                                     {:target (rename-keys (:target entity) {"key" "id"})})))))))
