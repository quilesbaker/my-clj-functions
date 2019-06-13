(ns my-clj-snippets.coins.core
  (:require [my-clj-snippets.coins.const :as c]
            [my-clj-snippets.coins.util :as u])
  (:gen-class))

;; Tree ?
;; mappings?

(defn amt->inexact-change "Returns non optimal coin change from specific amount.
  Doesn't return self coin."
  ([amount]
   (amt->inexact-change amount []))

  ([amount acc]
   (if (u/is-penny? amount)
     (conj acc c/P)
     (let [highest-coin (u/prev-coin amount)]
       (recur (- amount highest-coin) (conj acc highest-coin))))))

(defn amt->change "Returns optimal coin change from specific amount.
  Can return self coin."
  ([amount]
   (amt->change amount []))

  ([amount acc]
   (if (zero? amount)
     acc
     (let [highest-coin (u/highest-coin-that-fits amount)]
       (recur (- amount highest-coin) (conj acc highest-coin))))))

(defn inexact-change? [amount coin]
  (and
   (not (some nil? [amount coin]))
   (not= amount coin)
   (>= (- amount coin) 0)))

(defn amt->change-p [value]
  (cond
    (u/is-coin? value) (c/mappings value)
    :else (amt->change value)))

(comment "
   maps through vector
   sample input [26] (if 26, surround by []), acc = []
   if all Penny, done, return acc; which is a set.
   else, if first mapped element is higer than a coin, convert to coin equivalent
   the result of first fn is added to acc, then we call the resulting vector recursively
   so [26] -> [25 1], this version is easy
   now the input is [25 1] we map again. for each one that isnt a penny, get the conversion of it so:
   [25 1] -> [10 10 5 1]; add to acc. then recur again with this result.
   ;; ^ this is key, 25 doesnt return a 25 coin, but the next coin.
   ;; ^ we insert into original vector, so that they are merged and not nested vectors.
   [10 10 5 1] -> [5 5 10 5 1] call again, but also rest of mapped elements in vector:
                  [5 5 5 5 5 1]
                  [1 1 1 1 1 5 5 5 5 1]
                  [1 1 1 1 1 1 1 1 1 1 5 5 5 1]
                  [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 5 5 1]
                  [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 5 1]
                  [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1]
          all intermediary states are called by map and recur for each elem.
          hopefully acc covers everything when we're done. else, with no acc, just print each yielding result..
")

(comment "
replace in place:
[25 25 4] -> [10 10 5 25 4]
")

(defn fit-in [vector index item]
  (concat (take index vector)
          [item]
          (drop (inc index) vector)))

(defn fit-coll [vector index coll]
  (concat (take index vector)
          coll
          (drop (inc index) vector)))

(defn coin-cha
  "coin challenge"
  [purse]
  (if (u/all-pennies? purse)
    nil
    (map-indexed (fn [idx money] (when (not (u/is-penny? money))
                                   (println "letting")
                                   (let [res (fit-coll purse idx (amt->change-p money))]
                                     (println res)
                                     ;; (println (fit-coll purse idx res))
                                     (coin-cha res)
                                     ;; should call with replaced in place result, not just current coins
                                     )))
                 purse))
  )


#_(defn get-change-v1
  ;; (let) [purse (amt->change amount)] ;; 100 -> now [25 25 25 25]

  ;; (conj (into [] (map amt->inexact-change purse)) purse)

  ([purse]
   (get-change purse []))

  ([purse universe]
   (println "purse:" purse)
   ;; (let [bigger-purse])
   (conj universe (for [coin purse]
                    (do (println "a coin:" coin)
                        (if (vector? coin)
                          (if (u/all-pennies? coin)
                            (conj purse coin)
                            (get-change (amt->inexact-change coin) universe))
                          ;; numbers
                          (let [new-purse (amt->inexact-change coin)]
                            (if (u/all-pennies? new-purse)
                              (conj universe new-purse)
                              (get-change new-purse (conj universe new-purse)))))

                        ;; (get-change (if (vector? coin)
                        ;;               coin
                        ;;               (amt->inexact-change coin)))

                        )))))

#_"
[25 25 10 5 1 1 1]
for each, call fn
if 1, we're done: return that
if self, go to next amount for cond
next amount, call conversion:
generates a vector for example for 25: [10 10 5]
map call again this vector.
"

#_(defn change-but
  ([purse]
   ;; (println "first default call with missing args")
   (change-but purse (first purse) []))
  ([purse curr-coin acc]
   ;; (println "change-but args" purse curr-coin acc)
   (if (all-pennies? purse)
     purse
     (for [coin purse]
       (do
         ;; (println "coin" coin)
         (cond
           (is-penny? coin) P

           (unexact-change? curr-coin coin) (do
                                              ;; (println "unexact change")
                                              (change-but (amt->change (- curr-coin coin)) coin (conj acc coin))) ;; here call change-but on res from amt->change

           (coin-set coin) (change-but purse (prev-coin coin) acc)

           ;; other conditions here.. revise conditions above...

           ;; (>= (- amount P) 0) (amt->change (- amount P) (conj acc P))
           :else acc))))))

#_(defn decide
  [purse curr-coin acc]
  (for [coin purse]
    (cond
      (= coin P) P
      true 3
      ;; (= coin curr-coin) (change-but purse (prev-coin coin) acc)
      (unexact-change? curr-coin coin) (amt->change (- curr-coin coin) (conj acc coin)) ;; here call change-but on res from amt->change

      ;; (>= (- amount P) 0) (amt->change (- amount P) (conj acc P))
      :else acc)))

 #_"I have coins in my [] purse
   [25 5 1] ;; a Quarter, a Nickel, and a Penny
   if not a P, but a coin, can be deconstructed into more coins
   for each coin, convert into coin array w amt->inexact-change
   then.... vector of vectors... for each one, convert each coin to smaller chuncks..... o.0"

;; purse: amt->change(100) = [25 25 25 25]

#_"
 coins(10) -> [10]
 [10] -> for each, coins
 not all 1? for each, to coins
"

#_(defn get-change
  ([purse]
   (get-change purse [purse]))
  ([purse purses] ;; [] and [[]]
   (if (u/all-pennies? purse)
     purses

     (for [coin purse]
          (amt->inexact-change coin)
          )

     #_(let [[head tail] purse]
       next-purse (into purses (amt->inexact-change head))
       )

     )))
