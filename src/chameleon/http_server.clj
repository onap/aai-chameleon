(ns chameleon.http-server
  (:require [org.httpkit.server :refer [run-server]]
            [integrant.core :as ig]))

(defmethod ig/init-key :chameleon/http-server  [_ {:keys [port handler]}]
  (run-server handler {:port port}))

(defmethod ig/halt-key! :chameleon/http-server  [_ server]
  (server :timeout 100))
