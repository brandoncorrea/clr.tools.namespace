;; Copyright (c) Stuart Sierra, 2012. All rights reserved. The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution. By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license. You must not
;; remove this notice, or any other, from this software.

(ns ^{:author "Stuart Sierra, modified for ClojureCLR by David Miller"
      :doc "Parse Clojure namespace (ns) declarations and extract
  dependencies."}
  clojure.tools.namespace.parse
  (:require [clojure.set :as set]))

(defn comment?
  "Returns true if form is a (comment ...)"
  [form]
  (and (list? form) (= 'comment (first form))))

(defn ns-decl?
  "Returns true if form is a (ns ...) declaration."
  [form]
  (and (list? form) (= 'ns (first form))))

(defn read-ns-decl
  "Attempts to read a (ns ...) declaration from a
  java.io.PushbackReader, and returns the unevaluated form. Returns
  nil if read fails or if a ns declaration cannot be found. The ns
  declaration must be the first Clojure form in the file, except for
  (comment ...) forms."
  [rdr]
  (try
   (loop [] (let [form (doto (read rdr) str)]
              (cond
               (ns-decl? form) form
               (comment? form) (recur)
               :else nil)))
       (catch Exception e nil)))

;;; Parsing dependencies

(defn- deps-from-libspec [prefix form]
  (cond (list? form)
          (apply set/union
                 (map (fn [f] (deps-from-libspec
                               (symbol (str (when prefix (str prefix "."))
                                            (first form)))
                               f))
                      (rest form)))
	(vector? form)
          (deps-from-libspec prefix (first form))
	(symbol? form)
          #{(symbol (str (when prefix (str prefix ".")) form))}
	(keyword? form)
          #{}
	:else
          (throw (ArgumentException.                                             ;;; IllegalArgumentException.
                  (pr-str "Unparsable namespace form:" form)))))

(defn- deps-from-ns-form [form]
  (when (and (list? form)
	     (contains? #{:use :require} (first form)))
    (apply set/union (map #(deps-from-libspec nil %) (rest form)))))

(defn deps-from-ns-decl
  "Given an (ns...) declaration form (unevaluated), returns a set of
  symbols naming the dependencies of that namespace.  Handles :use and
  :require clauses but not :load."
  [decl]
  (apply set/union (map deps-from-ns-form decl)))