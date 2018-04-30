(ns chameleon.route
  (:require [org.httpkit.client :as kitclient]))

(defn- interpret-response
  "Print out the response from the Gallifrey server"
  [key response]
  (let [{:keys [status body]}@response]
    (println "Response for request with key " key " resulted in status " status
             " with body " body )))

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

(defn assert-create
  "Creates an entity in Gallifrey with an initial set of assertions coming from the provided payload"
  [host actor type key payload & [time-dimensions]]
  (kitclient/request {
                      :url (str "https://" host "/" type "/" key)
                      :method :put
                      :query-params (into {"actor" actor "create" "true"} time-dimensions)
                      :body payload
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-update
  "Update an entity in Gallifrey with a set of assertions coming from the provided payload"
  [host actor type key payload & [time-dimensions]]
  (kitclient/request {
                      :url (str "https://" host "/" type "/" key)
                      :method :put
                      :query-params (into  {"actor" actor "changes-only" "true"} time-dimensions)
                      :body payload
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-delete
  "Assert a deletion for an entity in Gallifrey based on the provided key."
  [host actor type key & [time-dimensions]]
  (kitclient/request {
                      :url (str "https://" host "/" type "/" key)
                      :method :delete
                      :query-params (into  {"actor" actor} time-dimensions)
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-gallifrey [host actor type payload]
  "Propagates an assertion to Gallifrey based off of an event payload coming in from the event service."
  (let [{:keys [meta body]} payload
        {:keys [key operation time]} meta]
    (println operation " "  type "  with key " key)
    (interpret-response key (case operation
                              "CREATE"
                              (assert-create host actor type key body time)
                              "UPDATE"
                              (assert-update host actor type key body time)
                              "DELETE"
                              (assert-delete host actor type key time)))))
