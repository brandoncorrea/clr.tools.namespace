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
                        [nine :only (c d)]
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
            [nine :only (c d)]
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