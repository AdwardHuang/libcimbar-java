# Third-party notices

## libcimbar embedded bitmap resources

The embedded bitmap data in `src/main/resources/cimbar/original-bitmaps-base91.txt` is derived from `sz3/libcimbar`, licensed under the Mozilla Public License 2.0 (MPL-2.0).
Source: https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/bitmaps.h

## Base91 decoder

`com.hrz.libcimbar.asset.Base91` is a Java implementation of the decoding algorithm and custom alphabet from `sz3/libcimbar/src/third_party_lib/base91/base.hpp` by rlyeh (2011–2016), which states zlib/libpng licensing for the Base project and includes the basE91 implementation by Joachim Henke (2000–2006), under the BSD-style terms reproduced in that upstream header.
Source: https://github.com/sz3/libcimbar/blob/master/src/third_party_lib/base91/base.hpp

This Java implementation is a clean-language port of the decoder logic; it does not include the upstream C++ header.
