;; Copyright © 2022 CicadaBank

;; Permission is hereby granted, free of charge, to any person obtaining a copy of
;; this software and associated documentation files (the "Software"), to deal in
;; the Software without restriction, including without limitation the rights to
;; use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
;; the Software, and to permit persons to whom the Software is furnished to do so,
;; subject to the following conditions:

;; The above copyright notice and this permission notice shall be included in all
;; copies or substantial portions of the Software.

;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
;; FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
;; COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
;; IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
;; CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(ns bips.btc-utils
  (:require
    [alphabase.base58 :as base58]
    [buddy.core.codecs :as codecs]
    [clj-commons.digest :as digest])
  (:import
    org.apache.commons.codec.binary.Hex))

(defn privatekey->wif
  "Convert an hexadecimal encoded private key to WIF"
  [private-key network & compressed]
  (let [prefix (if (= :mainnet network)
                 "80"
                 "EF")
        suffix (if (first compressed)
                 "01"
                 "")]
    (base58/encode (codecs/hex->bytes (str (-> private-key
                                               (#(str prefix % suffix)))
                                           (-> private-key
                                               (#(str prefix % suffix))
                                               (codecs/hex->bytes)
                                               (digest/sha-256)
                                               (codecs/hex->bytes)
                                               (digest/sha-256)
                                               (#(take 8 %))
                                               (#(reduce str %))))))))
