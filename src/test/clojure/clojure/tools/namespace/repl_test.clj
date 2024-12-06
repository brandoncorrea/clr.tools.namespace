(ns clojure.tools.namespace.repl-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.repl :as repl]))

(defn current-time-millis []
  (long
    (/ (- (.-Ticks DateTime/UtcNow)
          (.-Ticks DateTime/UnixEpoch))
       TimeSpan/TicksPerMillisecond)))

(deftest t-repl-scan-time-component
  (let [before (current-time-millis)
        scan   (repl/scan {:platform find/clj})
        after  (current-time-millis)
        time   (::dir/time scan)]
    (is (<= before time after))
    (is (integer? (::dir/time scan)))))
