(ns chameleon.core
  (:require [clojure.spec.alpha :as s]))

(defn conform-multiple
  [& spec-form-pair]
  (if (s/valid? :chameleon.specs/spec-form-pair spec-form-pair)
    (->> spec-form-pair
         (partition 2)
         (map (fn [[sp form]]
                (when (s/invalid? (s/conform sp form))
                  (s/explain-data sp form))))
         (remove nil?))
    (s/explain-data ::spec-form-pair spec-form-pair)))
