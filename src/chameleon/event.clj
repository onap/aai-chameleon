(ns chameleon.event
  (:require [chameleon.txform]
            [chameleon.route]
            [integrant.core :as ig])
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
                                 (let [event (.next it)]
                                   (processor gallifrey-host event))))))))
    ))
