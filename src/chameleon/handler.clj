(ns chameleon.handler
  (:require [chameleon.route :as c-route]
            [chameleon.aai-processor]
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
            [integrant.core :as ig]))

(declare handler)

(defonce ^:private g-host (atom nil))

(defmethod ig/init-key :chameleon/handler  [_ {:keys [gallifrey-host]}]
  (reset! g-host gallifrey-host)
  handler)

(defmethod ig/halt-key! :chameleon/handler  [_ _]
  (reset! g-host nil))

(declare serialize de-serialize)

(defresource resource-endpoint [type id]
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :exists? (fn [ctx]
             (let [resource (c-route/query @g-host id type (-> ctx
                                                               :request
                                                               :params
                                                               (select-keys [:t-t :t-k])))] ; Only pass through the allowable set of keys
               (when (= (:status resource) 200)
                 {::resource (-> resource
                                 :body
                                 json/parse-string
                                 (dissoc "_meta")
                                 (chameleon.aai-processor/from-gallifrey))})))
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

(def handler
  (-> app-routes
      (wrap-defaults api-defaults)))


;;; Implementation

(defn- serialize
  [e]
  (compact
   (update e :_meta #(map-vals
                      (fn [m]
                        (map-vals str m)) %))))

(defn- de-serialize
  [e]
  e)
