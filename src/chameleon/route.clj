(ns chameleon.route
  (:require [org.httpkit.client :as kitclient]
            [chameleon.logging :as log]
            [ring.util.http-status :as hs]))

(defn query
  "Retrieve an entity referenced by id at the provided host. Optionally provide
  a time 't-k' that defines a query based on when the system knew about the
  state of the entity."
  [host key type & [gallifrey-params]]
  @(kitclient/request {
                       :url (str "https://" host "/" type "/" key)
                       :method :get
                       :query-params gallifrey-params
                       :insecure? true
                       :keepalive 300
                       :timeout 20000}))

(defn assert-create!
  "Creates an entity in Gallifrey with an initial set of assertions coming from the provided payload"
  [host actor type key payload & [time-dimensions]]
  (kitclient/request {:url (str "https://" host "/" type "/" key)
                      :method :put
                      :query-params (into {"actor" actor "create" "true"} time-dimensions)
                      :body payload
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-update!
  "Update an entity in Gallifrey with a set of assertions coming from the provided payload"
  [host actor type key payload & [time-dimensions]]
  (kitclient/request {:url (str "https://" host "/" type "/" key)
                      :method :put
                      :query-params (into  {"actor" actor "changes-only" "true"} time-dimensions)
                      :body payload
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-delete!
  "Assert a deletion for an entity in Gallifrey based on the provided key."
  [host actor type key & [time-dimensions]]
  (kitclient/request {:url (str "https://" host "/" type "/" key)
                      :method :delete
                      :query-params (into {"actor" actor} time-dimensions)
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-gallifrey!
  [host actor type payload & [e-logger a-logger]]
  "Propagates an assertion to Gallifrey based off of an event payload coming in from the event service."
  (let [{:keys [meta body]} payload
        {:keys [key operation time]} meta
        _ (log/info e-logger "GALLIFREY_ASSERTION" (mapv str [operation type key]))
        g-assert (case operation
                   "CREATE" (assert-create! host actor type key body time)
                   "UPDATE" (assert-update! host actor type key body time)
                   "DELETE" (assert-delete! host actor type key time))
        {:keys [status body]} @g-assert]
    (log/info e-logger "RESPONSE" (mapv str [operation key status body]))
    (if (and (>= status 200) (<= status 299))
      (log/info e-logger "GALLIFREY_ASSERTED" ["SUCCEEDED" (str type) (str key)])
      (log/info e-logger "GALLIFREY_ASSERTED" ["FAILED" (str type) (str key)]))
    (log/info a-logger "RESPONSE" (mapv str [operation key status body])
              :fields {:response-code status :response-description (hs/get-name status)})))
