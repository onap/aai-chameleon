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
  [host key & [k]]
  @(kitclient/request {
                       :url (str "https://" host "/entity/" key)
                       :method :get
                       :query-params (if-let [t-k k] {"t-k" t-k})
                       :insecure? true
                       :keepalive 300
                       :timeout 1000}))

(defn assert-create
  "Creates an entity in Gallifrey with an initial set of assertions coming from the provided payload"
  [host actor key payload]
  (print "Final: " payload)
  (kitclient/request {
                      :url (str "https://" host "/entity/" key)
                      :method :put
                      :query-params {"actor" actor "create" "true"}
                      :body payload
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-update
  "Update an entity in Gallifrey with a set of assertions coming from the provided payload"
  [host actor key payload]
  (kitclient/request {
                      :url (str "https://" host "/entity/" key)
                      :method :put
                      :query-params {"actor" actor "changes-only" "true"}
                      :body payload
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-delete
  "Assert a deletion for an entity in Gallifrey based on the provided key."
  [host actor key]
  (kitclient/request {
                      :url (str "https://" host "/entity/" key)
                      :method :delete
                      :query-params {"actor" actor}
                      :insecure? true
                      :keepalive 300
                      :timeout 1000}))

(defn assert-gallifrey [host actor payload]
  "Propagates an assertion to Gallifrey based off of an event payload coming in from the event service."
  (let [{:keys [meta body]} payload
        {:keys [key operation]} meta]
    (println operation " entity with key " key)
    (interpret-response key (case operation
                              "CREATE"
                              (assert-create host actor key body)
                              "UPDATE"
                              (assert-update host actor key body)
                              "DELETE"
                              (assert-delete host actor key)))))
