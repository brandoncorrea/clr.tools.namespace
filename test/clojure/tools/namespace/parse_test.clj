(ns clojure.tools.namespace.parse-test
  (:use [clojure.test :only (deftest is)]
        [clojure.tools.namespace.parse :only (deps-from-ns-decl
                                              read-ns-decl)]))

(def ns-decl-prefix-list
  '(ns com.example.one
     (:require (com.example two
                            [three :as three]
                            [four :refer (a b)])
               (com.example.sub [five :as five]
                                six))
     (:use (com.example seven
                        [eight :as eight]
                        (nine :only (c d))
                        [ten]))))

;; Some people like to write prefix lists as vectors, not lists. The
;; use/require functions accept this form.
(def ns-decl-prefix-list-as-vector
  '(ns com.example.one
     (:require [com.example
                two
                [three :as three]
                [four :refer (a b)]]
               [com.example.sub
                [five :as five]
                six])
     (:use [com.example
            seven
            [eight :as eight]
            (nine :only (c d))
            [ten]])))

(def deps-from-prefix-list
  '#{com.example.two
     com.example.three
     com.example.four
     com.example.sub.five
     com.example.sub.six
     com.example.seven
     com.example.eight
     com.example.nine
     com.example.ten})

(deftest t-prefix-list
  (is (= deps-from-prefix-list
         (deps-from-ns-decl ns-decl-prefix-list))))

(deftest t-prefix-list-as-vector
  (is (= deps-from-prefix-list
         (deps-from-ns-decl ns-decl-prefix-list-as-vector))))

(deftest t-no-deps-returns-empty-set
  (is (= #{} (deps-from-ns-decl '(ns com.example.one)))))
  
(def multiple-ns-decls
  '((ns one)
    (ns two (:require one))
    (ns three (:require [one :as o] [two :as t]))))

(def multiple-ns-decls-string
" (println \"Code before first ns\")
  (ns one)
  (println \"Some code\")
  (defn hello-world [] \"Hello, World!\")
  (ns two (:require one))
  (println \"Some more code\")
  (ns three (:require [one :as o] [two :as t]))")

(deftest t-read-multiple-ns-decls
  (with-open [rdr (clojure.lang.PushbackTextReader.                              ;;; java.io.PushbackReader.
                   (System.IO.StringReader. multiple-ns-decls-string))]          ;;; java.io.StringReader.
    (is (= multiple-ns-decls
           (take-while identity (repeatedly #(read-ns-decl rdr)))))))
		   
(def ns-docstring-example
  "The example ns declaration used in the docstring of clojure.core/ns"
  '(ns foo.bar
     (:refer-clojure :exclude [ancestors printf])
     (:require (clojure.contrib sql combinatorics))
     (:use (my.lib this that))
     (:import (java.util Date Timer Random)
              (java.sql Connection Statement))))

(def deps-from-ns-docstring-example
  '#{clojure.contrib.sql
     clojure.contrib.combinatorics
     my.lib.this
     my.lib.that})

(deftest t-ns-docstring-example
  (is (= deps-from-ns-docstring-example
         (deps-from-ns-decl ns-docstring-example))))

(def require-docstring-example
  "The example ns declaration used in the docstring of
  clojure.core/require"
  '(ns (:require (clojure zip [set :as s]))))

(def deps-from-require-docstring-example
  '#{clojure.zip
     clojure.set})

(deftest t-require-docstring-example
  (is (= deps-from-require-docstring-example
         (deps-from-ns-decl require-docstring-example))))
		 
(def multiple-clauses
  "Example showing more than one :require or :use clause in one ns
  declaration, which clojure.core/ns allows."
  '(ns foo.bar
     (:require com.example.one)
     (:import java.io.File)
     (:require (com.example two three))
     (:use (com.example [four :only [x]]))
     (:use (com.example (five :only [x])))))

(def deps-from-multiple-clauses
  '#{com.example.one
     com.example.two
     com.example.three
     com.example.four
     com.example.five})

(deftest t-deps-from-multiple-clauses
  (is (= deps-from-multiple-clauses
         (deps-from-ns-decl multiple-clauses))))		 