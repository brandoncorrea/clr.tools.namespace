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

(defn- read-clj
  "Calls clojure.core/read. If reader conditionals are
  supported (Clojure 1.7) then adds options {:read-cond :allow}."
  [rdr]
  (if (resolve 'clojure.core/reader-conditional?)
    (read {:read-cond :allow} rdr)
    (read rdr)))
 
(defn- force-errors
  "Forces reader errors to be thrown immediately. Some versions of
  Clojure accept invalid forms in the reader and only throw an
  exception when they are printed.
  See http://dev.clojure.org/jira/browse/TNS-1"
  [form]
  (str form) ; str forces errors
  form)
 
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
  the first top-level ns form found. Returns nil if read fails or if a
  ns declaration cannot be found. Note that read can execute code
  (controlled by *read-eval*), and as such should be used only with
  trusted sources."
  [rdr]
  {:pre [(instance? clojure.lang.PushbackTextReader rdr)]}                  ;;; java.io.PushbackReader
  (try
   (loop []
     (let [form (force-errors (read-clj rdr))]
       (if (ns-decl? form)
         form
         (recur))))
   (catch Exception e nil)))

;;; Parsing dependencies

(defn- prefix-spec?
  "Returns true if form represents a libspec prefix list like
  (prefix name1 name1) or [com.example.prefix [name1 :as name1]]"
  [form]
  (and (sequential? form)  ; should be a list, but often is not
       (symbol? (first form))
       (not-any? keyword? form)
       (< 1 (count form))))  ; not a bare vector like [foo]

(defn- option-spec?
  "Returns true if form represents a libspec vector containing optional
  keyword arguments like [namespace :as alias] or
  [namespace :refer (x y)] or just [namespace]"
  [form]
  (and (sequential? form)  ; should be a vector, but often is not
       (symbol? (first form))
       (or (keyword? (second form))  ; vector like [foo :as f]
           (= 1 (count form)))))  ; bare vector like [foo]

(defn- deps-from-libspec [prefix form]
  (cond (prefix-spec? form)
           (mapcat (fn [f] (deps-from-libspec
                           (symbol (str (when prefix (str prefix "."))
                                        (first form)))
                           f))
                  (rest form))
	(option-spec? form)
          (deps-from-libspec prefix (first form))
	(symbol? form)
          (list (symbol (str (when prefix (str prefix ".")) form)))
	(keyword? form)     ; Some people write (:require ... :reload-all)
          nil
	:else
          (throw (ArgumentException.                                             ;;; IllegalArgumentException.
                  (pr-str "Unparsable namespace form:" form)))))

(defn- deps-from-ns-form [form]
  (when (and (sequential? form)  ; should be list but sometimes is not
	     (contains? #{:use :require 'use 'require} (first form)))
    (mapcat #(deps-from-libspec nil %) (rest form))))

(defn deps-from-ns-decl
  "Given an (ns...) declaration form (unevaluated), returns a set of
  symbols naming the dependencies of that namespace.  Handles :use and
  :require clauses but not :load."
  [decl]
  (set (mapcat deps-from-ns-form decl)))