(ns chameleon.event
  (:require [integrant.core :as ig]
            [clojure.string :refer [starts-with?]])
  (:import [org.onap.aai.event.client DMaaPEventConsumer]))

(defmethod ig/init-key :chameleon/event
  [_ {:keys [event-config gallifrey-host]}]
  (let [{:keys [host topic motsid pass consumer-group consumer-id timeout batch-size type processor]} (:aai event-config)
        event-processor (DMaaPEventConsumer. host topic motsid pass consumer-group consumer-id timeout batch-size type)]
    (println "Event processor for AAI  created. Starting event polling on " host  topic)
    (.start (Thread. (fn [] (while true
                             (let  [it (.iterator (.consume event-processor))]
                               (println "Polling...")
                               (while (.hasNext it)
                                 (try (let [event (.next it)]
                                        (if (not (starts-with? event "DMAAP")) ;Temporarily added for current version of dmaap client
                                          (processor gallifrey-host event)))
                                      (catch Exception e (println (str "Unexpected exception during processing: " (.getMessage e)))))))))))
    ))
