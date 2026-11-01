# Third-party notices

## libcimbar embedded bitmap resources

The embedded bitmap data in `src/main/resources/cimbar/original-bitmaps-base91.txt` is derived from `sz3/libcimbar`, licensed under the Mozilla Public License 2.0 (MPL-2.0).
Source: https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/bitmaps.h

## Base91 decoder

`com.hrz.libcimbar.asset.Base91` is a Java implementation of the decoding algorithm and custom alphabet from `sz3/libcimbar/src/third_party_lib/base91/base.hpp` by rlyeh (2011–2016), which states zlib/libpng licensing for the Base project and includes the basE91 implementation by Joachim Henke (2000–2006), under the BSD-style terms reproduced in that upstream header.
Source: https://github.com/sz3/libcimbar/blob/master/src/third_party_lib/base91/base.hpp

This Java implementation is a clean-language port of the decoder logic; it does not include the upstream C++ header.

## Symbol matching algorithm

`com.hrz.libcimbar.symbol.SymbolMatcher` is a Java-language adaptation of the symbol-candidate matching logic in `sz3/libcimbar/src/lib/cimb_translator/CimbDecoder.cpp` (`CimbDecoder::get_best_symbol` and `decode_symbol`), licensed under the Mozilla Public License 2.0 (MPL-2.0). The candidate iteration order is defined by `src/lib/image_hash/ahash_result.h`.
Source: https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbDecoder.cpp


## Reed-Solomon algorithm

`com.hrz.libcimbar.ecc.ReedSolomonCodec` is a clean Java implementation of the GF(256) Reed-Solomon parameters and wrapper behavior used by `sz3/libcimbar`'s `ReedSolomon.h` and bundled `libcorrect` API. The implementation does not include or link the native libcorrect C source. Upstream project files are licensed under MPL-2.0; libcorrect carries its own license in the upstream repository.
Sources: https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/ReedSolomon.h and https://github.com/sz3/libcimbar/tree/master/src/third_party_lib/libcorrect
