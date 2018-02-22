(ns chameleon.server
  (:require [chameleon.config :refer [config]]
            [chameleon.handler :refer [handler]]
            [config.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [integrant.core :as ig])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :http-port) "8082"))
        system-config (read-string (slurp (System/getenv "CONFIG_LOCATION" )))
        event-config (:event-config system-config)
        route-config (:gallifrey-host system-config)]
    (println "Listening on port" port)
    (ig/init (config {
                      :event-config event-config
                      :gallifrey-host route-config
                      :http-port port}))))
