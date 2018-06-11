(ns chameleon.logging
  (:require [camel-snake-kebab.core :as cs]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s])
  (:import [org.onap.aai.cl.api Logger LogFields LogLine]
           [org.onap.aai.cl.eelf LoggerFactory LogMessageEnum AaiLoggerAdapter AuditLogLine]
           [org.onap.aai.cl.mdc MdcContext MdcOverride]
           [com.att.eelf.i18n EELFResourceManager]
           [clojure.lang.IFn]
           [org.slf4j MDC]))

(declare LOGLINE_DEFINED_FIELDS error-logger audit-logger ->java string->enum logfields)

(defmethod ig/init-key :chameleon/loggers
  [_ {:keys [logback logmsgs] :or {logback (.getPath (io/file "chameleon_logback.xml"))
                                   logmsgs "log/ChameleonMsgs"}}]
  (System/setProperty "logback.configurationFile" logback)
  (EELFResourceManager/loadMessageBundle logmsgs)
  [(error-logger "chameleon.loggging") (audit-logger "chameleon.loggging")])

(defn conform-multiple
  [& spec-form-pair]
  (if (s/valid? :chameleon.specs/spec-form-pair spec-form-pair)
    (->> spec-form-pair
         (partition 2)
         (map (fn [[sp form]]
                (when (s/invalid? (s/conform sp form))
                  (s/explain-data sp form))))
         (remove nil?))
    (s/explain-data :chameleon.specs/spec-form-pair spec-form-pair)))

(defn mdc-set!
  "Sets the global MDC context for the current thread."
  [m]
  (doseq [[k v] m] (MDC/put k v)))

(defn mdc-init!
  "Sets the global MDC context as required by the EELF logging library"
  [transaction-id service instance partner client-address]
  (MdcContext/initialize transaction-id service instance partner client-address))

(defn mdc-clear! [] (MDC/clear))

(defmacro with-mdc
  "Will set the global MDC context with the options (will convert from
  keywords to PascalCase), execute the log, and clear the MDC
  context."
  [opts & body]
  `(do
     (mdc-set! (transform-keys cs/->PascalCaseString ~opts))
     ~@body
     (mdc-clear)))

(defn mdc-override
  [m]
  (->java (fn [j k v] (.addAttribute j k v)) (new MdcOverride)
          (transform-keys cs/->PascalCaseString m)))

(defn error-logger
  [name]
  (.getLogger (LoggerFactory/getInstance) name))

(defn audit-logger
  [name]
  (.getAuditLogger (LoggerFactory/getInstance) name))

(defn logger?
  [logger]
  (instance? AaiLoggerAdapter logger))

(defn info
  [^AaiLoggerAdapter logger ^String enum msgs & {:keys [fields] :or {fields {}}}]
  (let [confirmed-specs (conform-multiple :logging/valid-fields fields :logging/msgs msgs
                                          :chameleon.specs/logger logger)]
    (if (empty? confirmed-specs)
      (.info logger (string->enum enum) (logfields fields) (into-array java.lang.String msgs))
      confirmed-specs)))

(defn debug
  [^AaiLoggerAdapter logger ^String enum  msgs]
  (.debug logger (string->enum enum) (into-array java.lang.String msgs)))

(defn valid-logfields?
  [m]
  (->> (keys m)
       (map cs/->SCREAMING_SNAKE_CASE_STRING)
       set
       (clojure.set/superset? (-> LOGLINE_DEFINED_FIELDS keys set))))

(def ^{:private true
       :doc "Adding these fields from \"org.onap.aai.cl.api.LogLine\"
  class. Right now there isn't a known way to use the ENUMs of an
  abstract JAVA class if they are not STATIC.  The field order is very
  specific and it should be maintained for the common logging library
  version \"1.2.2\".  For a better understanding, please look at the
  \"DefinedFields\" ENUMs in the org.onap.aai.cl.api.LogLine
  class"}

  LOGLINE_DEFINED_FIELDS
  (zipmap ["STATUS_CODE" "RESPONSE_CODE" "RESPONSE_DESCRIPTION" "INSTANCE_UUID"
           "SEVERITY" "SERVER_IP" "CLIENT_IP" "CLASS_NAME" "PROCESS_KEY"
           "TARGET_SVC_NAME" "TARGET_ENTITY" "ERROR_CODE" "ERROR_DESCRIPTION"
           "CUSTOM_1" "CUSTOM_2""CUSTOM_3" "CUSTOM_4"]
          (range)))

(defn- string->enum
  ([^String enum] (proxy [Enum LogMessageEnum] [enum (hash enum)]))
  ([^String enum ordinal] (proxy [Enum LogMessageEnum] [enum ordinal])))

(defn- ->java
  [f jc m]
  (reduce (fn [a [k v]] (f a k v) a)
          jc m))

(defn- logfields
  "Generate a \"LogFields\" object with all the fields set."
  [m]
  (->> m
       (transform-keys cs/->SCREAMING_SNAKE_CASE_STRING)
       (transform-keys #(string->enum % (LOGLINE_DEFINED_FIELDS %)))
       (->java (fn [j field v] (.setField j field v)) (new LogFields))))
