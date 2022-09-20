(ns bips.bip32
  (:require
    [buddy.core.codecs :as codecs]
    [buddy.core.mac :as mac]
    [bips.bip32-utils :refer [compress-public-key
                              decompressKey
                              hardened]]
    [clojure.math.numeric-tower :as math]
    [clojure.string :as str])
  (:import
    java.math.BigInteger
    org.web3j.crypto.Sign))

(defn derive-master-node
  [seed]
  (let [master-code
        (codecs/bytes->hex
          (mac/hash (codecs/hex->bytes seed) {:key (codecs/str->bytes "Bitcoin seed")
                                              :alg :hmac+sha512}))
        private-key (apply str (take 64 master-code))]
    {:private-key private-key
     :public-key (compress-public-key (.toString (Sign/publicKeyFromPrivate
                                                   (BigInteger. (apply str private-key)
                                                                16))
                                                 16))
     :chain-code (apply str (take-last 64 master-code))
     :depth 0}))

(defn CKDpriv [{k-par :private-key
                c-par :chain-code
                depth :depth} index]
  (let [K-par (compress-public-key
                (.toString (Sign/publicKeyFromPrivate
                             (BigInteger. (apply str k-par)
                                          16))
                           16))
        I (if (>= index (hardened 0))
            (mac/hash (codecs/hex->bytes (str "00"
                                              k-par
                                              (format "%08x" index)))
                      {:key (codecs/hex->bytes c-par)
                       :alg :hmac+sha512})
            (mac/hash (codecs/hex->bytes (str K-par
                                              (format "%08x" index)))
                      {:key (codecs/hex->bytes c-par)
                       :alg :hmac+sha512}))]
    {:private-key (.toString (.mod (.add (BigInteger. k-par 16)
                                         (BigInteger. 1 (byte-array (take 32 I))))
                                   (.getN Sign/CURVE_PARAMS))
                             16)
     :chain-code (codecs/bytes->hex (byte-array (take-last 32 I)))
     :index index
     :depth (+ depth 1)}))

(defn CKDpub [{K-par :public-key
               c-par :chain-code
               depth :depth} index]
  (if (>= index (hardened 0))
    (throw (Exception. "Cannot derive a public key for hardened child keys."))
    (let [I (mac/hash (codecs/hex->bytes (str K-par
                                              (format "%08x" index)))
                      {:key (codecs/hex->bytes c-par)
                       :alg :hmac+sha512})
          public-key (.getEncoded
                       (.add
                         (Sign/publicPointFromPrivate
                           (BigInteger. 1 (byte-array (take 32 I))))
                         (decompressKey (BigInteger. (apply str K-par) 16)
                                        (= 0 (mod (nth (codecs/hex->bytes K-par) 0) 2))))
                       true)]
      {:public-key (codecs/bytes->hex (byte-array (take-last 64 public-key)))
       :chain-code (codecs/bytes->hex (byte-array (take-last 32 I)))
       :index index
       :depth (+ depth 1)})))

(defn N [{k-par :private-key
          c-par :chain-code
          index :index
          depth :depth}]
  {:public-key (compress-public-key
                 (.toString
                   (Sign/publicKeyFromPrivate
                     (BigInteger. (apply str k-par) 16)) 16))
   :chain-code c-par
   :index index
   :depth depth})

(defmacro derive-path [seed chain-path]
  (let [path-parts (str/split chain-path #"/")]
    (if (= 1 (count path-parts))
      (if (= "m" (first path-parts))
        `(derive-master-node ~seed)
        `(throw (Exception. (str "Invalid path: " (first ~path-parts))))))))

(comment
  (derive-path "000102030405060708090a0b0c0d0e0f" "m"))
