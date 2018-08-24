(ns chameleon.config
  (:require [integrant.core :as ig]
            [chameleon.aai-processor :refer :all]))

(defn config
  [app-config]
  (let [conf {:chameleon/loggers (:log-config app-config)
              :chameleon/event
              {:event-config (assoc-in (:event-config app-config)
                                       [:aai :processor] from-spike)
               :gallifrey-host (:gallifrey-host app-config)
               :loggers (ig/ref :chameleon/loggers)}
              :chameleon/handler
              {:gallifrey-host (:gallifrey-host app-config)
               :gallifrey-transformer from-gallifrey
               :loggers (ig/ref :chameleon/loggers)}
              :chameleon/aai-processor
              {:provenance-attr :last-mod-source-of-truth
               :truth-attr :truth-time}
              :chameleon/http-server
              {:port (:http-port app-config)
               :handler (ig/ref :chameleon/handler)}}]
    (ig/load-namespaces conf)
    conf))
