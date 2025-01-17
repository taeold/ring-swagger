(ns ring.swagger.common
  (:require [plumbing.core :refer [fn-> dissoc-in]]))

(defn remove-empty-keys
  "removes empty keys from a map"
  [m] (into (empty m) (filter (fn-> second nil? not) m)))

(defn value-of
  "Extracts value of for var, symbol or returns itself"
  [x]
  (cond
    (var? x) (var-get x)
    (symbol? x) (eval x)
    :else x))

(defn extract-parameters
  "Extract parameters from head of the list. Parameters can be:

   1. a map (if followed by any form) `[{:a 1 :b 2} :body]` => `{:a 1 :b 2}`
   2. list of keywords & values `[:a 1 :b 2 :body]` => `{:a 1 :b 2}`
   3. else => `{}`

   Returns a tuple with parameters and body without the parameters"
  [c]
  {:pre [(sequential? c)]}
  (if (and (map? (first c)) (> (count c) 1))
    [(first c) (rest c)]
    (if (keyword? (first c))
      (let [parameters (->> c
                         (partition 2)
                         (take-while (comp keyword? first))
                         (mapcat identity)
                         (apply array-map))
            form       (drop (* 2 (count parameters)) c)]
        [parameters form])
      [{} c])))

(defn plain-map?
  "checks whether input is a map, but not a record"
  [x] (and (map? x) (not (record? x))))

(defn update-in-or-remove-key
  ([m ks f] (update-in-or-remove-key m ks f nil?))
  ([m ks f iff]
    (let [v (f (get-in m ks))]
      (if-not (iff v)
        (assoc-in m ks v)
        (dissoc-in m ks)))))

(defn deep-merge
  "Recursively merges maps.
   If the first parameter is a keyword it tells the strategy to
   use when merging non-map collections. Options are
   - :replace, the default, the last value is used
   - :into, if the value in every map is a collection they are concatenated
     using into. Thus the type of (first) value is maintained."
  {:arglists '([strategy & values] [values])}
  [& values]
  (let [[values strategy] (if (keyword? (first values))
                            [(rest values) (first values)]
                            [values :replace])]
    (cond
      (every? map? values)
      (apply merge-with (partial deep-merge strategy) values)

      (and (= strategy :into) (every? coll? values))
      (reduce into values)

      :else
      (last values))))
