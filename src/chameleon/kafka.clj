(ns chameleon.kafka
  (:refer-clojure :exclude [send])
  (:require [clojure.core.async :as ca]
            [clojure.spec.alpha :as s]
            [chameleon.core :as core]
            [chameleon.logging :as log])
  (:import [java.util Properties]
           [org.apache.kafka.clients.consumer KafkaConsumer]
           [org.apache.kafka.clients.consumer ConsumerRecord]
           [org.apache.kafka.common.serialization StringDeserializer StringSerializer Serdes]))

(declare consume run-away)

(defrecord CljKafkaConsumer [config topic])


;; Public
(def deser StringDeserializer)
(def ser StringSerializer)

(defmacro threaded
  [name & body]
  `(try (.start (Thread. (fn [] ~@body) ~name))
        (catch OutOfMemoryError e#
          (throw (ex-info {:error "Could not allocate resources for asynchronous execution"})))))

(defn clj-kafka-consumer
  "Given a config, group-id, and a topic, return a CljKafkaConsumer"
  [config group-id topic error-logger]
  (let [valid? (core/conform-multiple :kafka/config config
                                      :chameleon.specs/string group-id
                                      :chameleon.specs/string topic)]
    (if-not (seq valid?)
      (CljKafkaConsumer. (assoc config "group.id" group-id) topic)
      (log/info error-logger "ERROR" (->> valid?
                                          (into ["SPEC ERROR"])
                                          (mapv str))))))

(defn subscribe
  "Given a CljKafkaConsumer, a channel, and a session timeout (in
  ms), return a channel.  The input channel is where the messages will
  be published."
  ([{:keys [config topic] :as conf} channel session-timeout error-logger]
   (subscribe conf channel session-timeout "Kafka-Subscriber-Thread"))
  ([{:keys [config topic]} channel session-timeout thread-name error-logger]
   (let [valid? (core/conform-multiple :kafka/timeout session-timeout
                                       :kafka/chan channel
                                       :kafka/config config
                                       :chameleon.specs/string topic)
         control-channel (ca/chan (ca/dropping-buffer 1))]
     (if-not (seq valid?)
       (do (threaded thread-name (consume config topic channel session-timeout control-channel))
           control-channel)
       (log/info error-logger "ERROR" (->> valid?
                                           (into ["SPEC ERROR"])
                                           (mapv str)))))))

;; Private

(defn- consumer-record->clojure
  [^ConsumerRecord consumer-record]
  {:topic (.topic ^ConsumerRecord consumer-record)
   :partition (.partition ^ConsumerRecord consumer-record)
   :offset (.offset ^ConsumerRecord consumer-record)
   :key (.key ^ConsumerRecord consumer-record)
   :value (.value ^ConsumerRecord consumer-record)})

(defn- send-with-sla-timeout
  "Consumer Records will always be a sequence. Try to put a message on
  the channel before the sla-timeout. Return true if a message was put
  on the channel or when there were no messages to put on the
  channel. Return false if there is a timeout."
  [chan sla-timeout messages]
  (if (seq messages)
    (let [sent? (ca/alt!! [[chan messages]] :sent
                          (ca/timeout sla-timeout) :timeout)]
      (if (= sent? :timeout)
        false
        true))
    true))

(defn- consume
  "Consume elements from a topic and put it on the given
  channel. Block until a given timeout to put messages on the channel
  or put sla-exception on the control channel (channel returned by
  this function). Once the message is put on the channel, it is
  considered safe to update the offset of the kafka consumer.  The
  `sla-timeout` is an agreement such that the data has to be taken off
  the channel within the given timeout. If failed to take the data,
  kaka consumer will be disconnected and an exception will be put on
  the control channel."
  [config topic chan sla-timeout control-channel]
  (let [consumer (KafkaConsumer. config)]
    (.subscribe ^KafkaConsumer consumer (list topic))
    (loop []
      (let [consumer-records (.poll consumer 0)]
        (when (and (->>  consumer-records
                         (s/conform  :kafka/consumer-record)
                         (map consumer-record->clojure)
                         (send-with-sla-timeout chan sla-timeout))
                   (try (.commitSync consumer)
                        true
                        (catch Exception e
                          (ca/>!! control-channel (.getMessage e))
                          false)))
          (recur))))
    (ca/>!! control-channel :sla-timeout)
    (.unsubscribe consumer)
    (.close consumer)))
