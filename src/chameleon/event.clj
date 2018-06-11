(ns chameleon.event
  (:require [integrant.core :as ig]
            [clojure.string :refer [starts-with?]]
            [chameleon.logging :as log])
  (:import [org.onap.aai.event.client DMaaPEventConsumer]))

(defmethod ig/init-key :chameleon/event
  [_ {:keys [event-config gallifrey-host loggers]}]
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
