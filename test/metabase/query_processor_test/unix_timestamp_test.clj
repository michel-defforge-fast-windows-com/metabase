(ns metabase.query-processor-test.unix-timestamp-test
  "Tests for UNIX timestamp support."
  (:require [metabase
             [driver :as driver]
             [query-processor-test :refer :all :as qp.test]]
            [clojure.test :refer :all]
            [metabase.test.data.datasets :as datasets]
            [metabase.test
             [data :as data]
             [util :as tu]]))

(deftest filter-test
  (datasets/test-drivers (qp.test/normal-drivers)
    (is (= 10
           ;; There's a race condition with this test. If we happen to grab a
           ;; connection that is in a session with the timezone set to pacific,
           ;; we'll get 9 results even when the above if statement is true. It
           ;; seems to be pretty rare, but explicitly specifying UTC will make
           ;; the issue go away
           (tu/with-temporary-setting-values [report-timezone "UTC"]
             (count (rows (data/dataset sad-toucan-incidents
                            (data/run-mbql-query incidents
                              {:filter   [:= [:datetime-field $timestamp :day] "2015-06-02"]
                               :order-by [[:asc $timestamp]]}))))))
        "There were 10 'sad toucan incidents' on 2015-06-02 in UTC")))

(deftest results-test
  (datasets/test-drivers (qp.test/normal-drivers)
    (is (= (cond
             (= :sqlite driver/*driver*)
             [["2015-06-01"  6]
              ["2015-06-02" 10]
              ["2015-06-03"  4]
              ["2015-06-04"  9]
              ["2015-06-05"  9]
              ["2015-06-06"  8]
              ["2015-06-07"  8]
              ["2015-06-08"  9]
              ["2015-06-09"  7]
              ["2015-06-10"  9]]

             (qp.test/tz-shifted-driver-bug? driver/*driver*)
             [["2015-06-01T00:00:00-07:00" 6]
              ["2015-06-02T00:00:00-07:00" 10]
              ["2015-06-03T00:00:00-07:00" 4]
              ["2015-06-04T00:00:00-07:00" 9]
              ["2015-06-05T00:00:00-07:00" 9]
              ["2015-06-06T00:00:00-07:00" 8]
              ["2015-06-07T00:00:00-07:00" 8]
              ["2015-06-08T00:00:00-07:00" 9]
              ["2015-06-09T00:00:00-07:00" 7]
              ["2015-06-10T00:00:00-07:00" 9]]

             (supports-report-timezone? driver/*driver*)
             [["2015-06-01T00:00:00-07:00" 8]
              ["2015-06-02T00:00:00-07:00" 9]
              ["2015-06-03T00:00:00-07:00" 9]
              ["2015-06-04T00:00:00-07:00" 4]
              ["2015-06-05T00:00:00-07:00" 11]
              ["2015-06-06T00:00:00-07:00" 8]
              ["2015-06-07T00:00:00-07:00" 6]
              ["2015-06-08T00:00:00-07:00" 10]
              ["2015-06-09T00:00:00-07:00" 6]
              ["2015-06-10T00:00:00-07:00" 10]]

             :else
             [["2015-06-01T00:00:00Z" 6]
              ["2015-06-02T00:00:00Z" 10]
              ["2015-06-03T00:00:00Z" 4]
              ["2015-06-04T00:00:00Z" 9]
              ["2015-06-05T00:00:00Z" 9]
              ["2015-06-06T00:00:00Z" 8]
              ["2015-06-07T00:00:00Z" 8]
              ["2015-06-08T00:00:00Z" 9]
              ["2015-06-09T00:00:00Z" 7]
              ["2015-06-10T00:00:00Z" 9]])
           (tu/with-temporary-setting-values [report-timezone "America/Los_Angeles"]
             (->> (data/dataset sad-toucan-incidents
                    (data/run-mbql-query incidents
                      {:aggregation [[:count]]
                       :breakout    [$timestamp]
                       :limit       10}))
                  rows (format-rows-by [identity int])))))))
