(ns chameleon.handler
  (:require [chameleon.route :as c-route]
            [utilis.map :refer [map-vals compact]]
            [liberator.core :refer [defresource]]
            [compojure.core :refer [GET PUT PATCH ANY defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response content-type]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.session :refer [wrap-session]]
            [cheshire.core :as json]
            [clj-time.format :as tf]
            [integrant.core :as ig]
            [chameleon.logging :as log]))

(declare handler)

(defonce ^:private g-host (atom nil))
(defonce ^:private g-transformer nil)

(defmethod ig/init-key :chameleon/handler  [_ {:keys [gallifrey-host loggers gallifrey-transformer]}]
  (reset! g-host gallifrey-host)
  (def g-transformer gallifrey-transformer)
  (handler loggers))

(defmethod ig/halt-key! :chameleon/handler  [_ _]
  (reset! g-host nil)
  (def g-transformer nil))

(declare serialize de-serialize)

(defresource resource-endpoint [type id]
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :exists? (fn [ctx]
             (let [resource (c-route/query @g-host id type  (-> ctx
                                                                :request
                                                                :params
                                                                (select-keys [:t-t :t-k])))] ; Only pass through the allowable set of keys
               (when (= (:status resource) 200)
                 {::resource (-> resource
                                 :body
                                 json/parse-string
                                 (dissoc "_meta")
                                 (g-transformer))})))
  :existed? (fn [ctx]
              (when-let [status (-> (c-route/query @g-host id type (-> ctx
                                                                       :request
                                                                       :params
                                                                       (select-keys [:t-t :t-k]))) ;Only pass through the allowable set of keys
                                    :status)]
                (= status 410)))
  :handle-ok ::resource)

(defroutes app-routes
  (GET "/entity/:id" [id] (resource-endpoint "entity" id))
  (GET "/relationship/:id" [id] (resource-endpoint "relationship"  id))
  (resources "/"))

(defn log-reqs
  [handler loggers]
  (let [[error-logger audit-logger] loggers]
    (fn [request]
      (log/mdc-init! (get-in request [:headers "X-TransactionId"]) "CHAMELEON"
                     "CHAMELEON_SERVICE" "ONAP" (:remote-addr request))
      (log/info error-logger "CHAMELEON_REQUEST" (mapv str ((juxt (comp name :request-method) :uri :remote-addr) request)))
      (let [resp (handler request)
            fields (->> ((juxt :status :body) resp)
                        (into ((juxt (comp name :request-method) :uri) request))
                        (mapv str))]
        (log/info error-logger "RESPONSE" fields)
        (log/info audit-logger "RESPONSE" fields)
        (log/mdc-clear!)
        resp))))

(defn handler
  [loggers]
  (-> app-routes
      (wrap-defaults api-defaults)
      (log-reqs loggers)))
