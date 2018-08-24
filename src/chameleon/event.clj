(ns chameleon.event
  (:require [integrant.core :as ig]
            [clojure.string :refer [starts-with?]]
            [chameleon.logging :as log]
            [clojure.core.async :as ca]
            [chameleon.kafka :as ck])
  (:import [org.onap.aai.event.client DMaaPEventConsumer KafkaEventConsumer]))

(declare from-dmaap from-kafka)

(defmethod ig/init-key :chameleon/event
  [_ {:keys [event-config gallifrey-host loggers]}]
  (case (:source event-config)
    :dmaap (from-dmaap event-config gallifrey-host loggers)
    :kafka (from-kafka event-config gallifrey-host loggers)
    :error))

(defn- from-dmaap
  [event-config gallifrey-host loggers]
  (let [{:keys [host topic motsid pass consumer-group consumer-id timeout batch-size type processor]} (:aai event-config)
        [error-logger audit-logger] loggers
        event-processor (DMaaPEventConsumer. host topic motsid pass consumer-group consumer-id timeout batch-size type)]
    (log/info error-logger "EVENT_PROCESSOR" [(format "AAI  created. Starting event polling on %s %s" host topic) ])
    (.start (Thread. (fn [] (while true
                             (let  [it (.iterator (.consume event-processor))]
                               (log/info error-logger "EVENT_PROCESSOR" ["Polling ..."])
                               (while (.hasNext it)
                                 (log/mdc-init! "SPIKE-EVENT" "CHAMELEON" "" "" gallifrey-host)
                                 (try (let [event (.next it)]
                                        ;;Temporarily added for current version of dmaap client
                                        (when-not (starts-with? event "DMAAP")
                                          (log/info error-logger "EVENT_PROCESSOR" [event])
                                          (processor gallifrey-host event error-logger audit-logger)
                                          (log/mdc-clear!)))
                                      (catch Exception e
                                        (println (str "Unexpected exception during processing: " (.getMessage e)))))))))))))

(defn- from-kafka
  [event-config gallifrey-host loggers]
  (let [{:keys [topic consumer-group processor kafka-config]} (:aai event-config)
        [error-logger audit-logger] loggers
        kfc (ck/clj-kafka-consumer kafka-config consumer-group topic)
        chan (ca/chan 5)
        error-chan (ck/subscribe kfc chan 30000 "Polling-Kafka-Thread")]
    (log/info error-logger "EVENT_PROCESSOR"
              [(format "AAI created. Starting polling a KAFKA Topic '%s' on '%s'" topic (kafka-config "bootstrap.servers"))])
    (ca/go-loop []
      (let [recs (ca/<! chan)]
        (log/mdc-init! "SPIKE-EVENT" "CHAMELEON" "" "" gallifrey-host)
        (if recs
          (do (doseq [r recs]
                (log/info error-logger "EVENT_PROCESSOR" [(str "Processing " (:value r))])
                (processor gallifrey-host (:value r) error-logger audit-logger)
                (log/info error-logger "EVENT_PROCESSOR" [(str "Processed Message " (:value r))])
                (log/mdc-clear!))
              (recur))
          (ca/<! error-chan))))))
