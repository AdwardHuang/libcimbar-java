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


## Decoder 双平面恢复流程

`com.hrz.libcimbar.decoder.FrameDataDecoder` 与 `CimbReader` 的两阶段读取结构，是对 `sz3/libcimbar` `Decoder.h`、`CimbReader.h/.cpp`、`bit_file/bitbuffer.h` 和 `Interleave.h` 的 Java 语言移植。上游项目相关代码采用 MPL-2.0。
Sources: https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/Decoder.h and https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbReader.cpp

## Fountain metadata and color correction

The Fountain metadata layout and color-correction algorithm are Java reimplementations based on `sz3/libcimbar` source files `src/lib/fountain/FountainMetadata.h`, `src/lib/cimb_translator/CimbReader.cpp`, and `src/lib/chromatic_adaptation/color_correction.h`. The upstream project is distributed under the Mozilla Public License 2.0. This Java implementation does not include or link OpenCV.

## Fountain decoder protocol boundary

`FountainDecoderStream` and `FountainDecoderSink` are Java-language adaptations of the packet framing, stream routing, duplicate-block filtering, progress, and recovery lifecycle in `sz3/libcimbar/src/lib/fountain/fountain_decoder_stream.h`, `fountain_decoder_sink.h`, and `FountainDecoder.h` (MPL-2.0). This release defines only a backend SPI; it does not include the Wirehair implementation or native Wirehair source.


## Wirehair GF(256 field specification

`com.hrz.libcimbar.fountain.wirehair.GF256` is a clean Java implementation of the GF(256) field arithmetic contract documented by Christopher A. Taylor's Wirehair project: polynomial `0x14D`, generator element `2`. No Wirehair C/C++ source is included or linked in this release. The upstream Wirehair project is distributed under the BSD 3-Clause license.
Source: https://github.com/catid/wirehair/blob/master/WirehairCodec.h

### Wirehair PCG / Peel row generation (0.2.7)

The Java implementation in `com.hrz.libcimbar.fountain.wirehair` follows the PCG generator, peel-row weight distribution, row-parameter initialization, and column-iteration algorithms in the vendored Wirehair source distributed by sz3/libcimbar. Wirehair is Copyright (c) 2012-2018 Christopher A. Taylor and is distributed under the BSD 3-Clause license. This project contains a Java reimplementation of the documented algorithms; it does not include the upstream C/C++ source files.

## libcimbar Tile encoder mapping

The `com.hrz.libcimbar.encoder.CimbEncoder` Java implementation follows the tile-index mapping in `sz3/libcimbar`, `src/lib/cimb_translator/CimbEncoder.cpp`, and uses the project's existing embedded original bitmap assets and `TileFactory`. The implementation is a clean Java port of the mapping behavior; no C++ source code is included. libcimbar is licensed under MPL-2.0; see https://github.com/sz3/libcimbar/blob/master/LICENSE.
