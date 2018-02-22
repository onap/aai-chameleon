(ns chameleon.txform
  (:require [cheshire.core :refer :all]
            [clojure.string :as str]))

(defn map-keywords
  "Maps all string based keys to keywords"
  [kmap]
  (into {} (for [[key value] kmap] [(keyword key) value])))

(defn flatten-key
  "Maps a parent-child pair to a period separated keyword"
  [parent child]
  (keyword (str (name parent) "." (name child))))

(defn flatten-entry
  "Flattens a nested map entry to a period separated keyword entry"
  [map key]
  (reduce #(assoc %1 (flatten-key key (first %2)) (second %2))
          {} (seq (key map))))
