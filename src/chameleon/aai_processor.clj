(ns chameleon.aai-processor
  (:require [chameleon.txform :refer :all]
            [chameleon.route :refer :all]
            [cheshire.core :refer :all]))

(defn from-gallifrey
  "Transforms Gallifrey response payloads into a format consumable by AAI-centric clients"
  [body]
  (->> body
       (map (fn [[k v]] [(clojure.string/split k #"\.") v]))
       ((fn [x] (reduce #(assoc-in %1 (first %2) (second %2) ) {} x)))))

(defn from-spike
  "Transforms Spike-based event payloads to a format accepted by Gallifrey for vertices and relationships"
  [gallifrey-host payload]
  (let [txpayload (map-keywords (parse-string payload))
        operation (:operation txpayload)
        entity-type (if (contains? txpayload :vertex)
                      :vertex
                      :relationship)
        entity (map-keywords (entity-type txpayload))
        key (:key entity)
        properties (assoc (:properties entity) :type (:type entity))]
    (assert-gallifrey gallifrey-host "aai" (if (= entity-type :vertex)
                                             {:meta {:key key :operation operation} :body (generate-string properties)}
                                             {:meta {:key key :operation operation} :body (generate-string (conj properties (flatten-entry entity :source) (flatten-entry entity :target)))}))))
