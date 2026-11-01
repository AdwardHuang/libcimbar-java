# libcimbar-java 0.2.4-SNAPSHOT

一个面向 Java 17 的纯 Java Maven 工程，整理目前已讨论的 libcimbar 图像识别基础模块。

## 环境

- JDK 17+
- Maven 3.8+
- 不需要 JNI、OpenCV、ZXing 或本地动态库

## 构建和运行

```bash
mvn clean test
mvn exec:java
```

传入图片运行预处理：

```bash
mvn exec:java -Dexec.args="D:/images/input.png"
```

## 当前包含

- `SymbolGridPreprocessor`：RGB 灰度化、可选锐化、自适应均值二值化。
- `AdaptiveThreshold`：滑动窗口均值阈值，边界复制。
- `BinaryImagePacker`：行优先、MSB-first 位打包。
- `BitMatrix` / `BitMatrixFactory`：线性 bit 视图。
- `AverageHash`：RGB 灰度转换、整数均值阈值、行优先 MSB-first Hash；支持显式阈值参数。
- `FuzzyAHash`：按 libcimbar 原版规则，从 7×7 / 10×10 二值网格提取九个偏移 Hash，支持 ALL / FAST 模式。
- `Base91`：移植 libcimbar 使用的 rlyeh 自定义 Base91 字母表与位打包解码。
- `OriginalBitmapAssets`：从原版 `bitmaps.h` 内嵌数据恢复 20 张 PNG（4 张 5×5、16 张 8×8），无需手工下载 PNG。
- `TileFactory`：优先读取外部 classpath PNG；缺失时自动使用内嵌原版 PNG，并按原版颜色/背景规则着色。
- `SymbolDictionary`：生成符号 Hash 字典。
- `SymbolMatcher`：按 libcimbar `CimbDecoder::get_best_symbol()` 的中心/边缘/角点顺序进行贪心汉明距离匹配，支持 FAST/ALL、cooldown、BitMatrix 入口和精确匹配提前返回。

## 原版 bitmap 资源

工程已经内置 libcimbar `src/lib/cimb_translator/bitmaps.h` 中的 20 张符号 PNG。资源以原版自定义 Base91 编码文本保存在 `src/main/resources/cimbar/original-bitmaps-base91.txt`，运行时由 `Base91` 解码并通过 JDK `ImageIO` 读取，不需要手工下载图片，也不需要 OpenCV/stb_image。

`TileFactory` 仍支持外部 classpath PNG 覆盖内嵌版本，便于后续替换和对照。内嵌资源覆盖 2-bit 的 `00..03` 和 4-bit 的 `00..0f`。

## 重要兼容性说明

这不是完整 libcimbar Java 移植，也不是可直接解码 libcimbar 图片的最终版本。当前工程是可构建的算法基础工程。以下部分尚未完成或尚未与原版逐像素/逐字节验证：

1. OpenCV `cvtColor`、`filter2D`、`adaptiveThreshold` 的所有边界和舍入细节。
2. OpenCV `resize(INTER_LINEAR)` 的所有优化路径与像素级一致性。
3. 原版 PNG 的像素级颜色/透明度边界核验，以及所有 symbol/color mode 的完整兼容验证。
4. 四角锚点检测与透视变换尚未实现；当前 `CimbReader` 已实现已矫正图像上的符号网格读取、drift 跟踪和 FloodDecodePositions 调度。
5. 自动锚点白色采样与 von Kries 色彩适应、基于 Fountain Header 的完整 CCM 求解，以及完整帧协议。
6. libcimbar 文件层 Wirehair/Zstd 协议。

因此，现阶段不要将它作为完整解码器接入 QOcr 的正式传输流程。当前颜色分类核心已移植，但尚未完成自动色彩校正；`FuzzyAHash` 已按原版 `ahash_result`、`bit_extractor` 和 `fuzzy_ahash(bitmatrix)` 的规则实现；合成网格测试验证了九个偏移位置与 FAST 顺序。`AverageHashOriginalVectorTest` 保留了原版测试向量；`OriginalBitmapAssetsTest` 进一步直接加载内嵌的真实 PNG，并对原版测试中的 8 个固定 Hash 值做了真实资源回归验证，同时检查 16 个 8×8 符号的最小两两 Hamming Distance 为 18。原版测试源文件：https://github.com/sz3/libcimbar/blob/master/src/lib/image_hash/test/averageHashTest.cpp。原版 `getTile` 的颜色和背景逻辑见：https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Common.cpp。当前已内置真实 bitmap，并通过固定 Hash 向量验证资源、`AverageHash` 和 `SymbolDictionary` 的基础输出；OpenCV 预处理的逐像素一致性仍需继续验证。

## Maven 集成（后续）

当 API 稳定后，可在 QOcr 的 `pom.xml` 中添加：

```xml
<dependency>
  <groupId>com.hrz</groupId>
  <artifactId>libcimbar-java</artifactId>
  <version>0.2.2-SNAPSHOT</version>
</dependency>
```

本地安装：

```bash
mvn clean install
```


## 0.1.3 修复

`AverageHash` 对单通道灰度图直接读取 Raster 原始 8-bit 采样值，避免 `BufferedImage.getRGB()` 的灰度颜色空间转换改变阈值比较结果。RGB 图像仍走 RGB 转灰度路径。16-bit 单通道输入会明确拒绝，避免静默截断。


## 0.1.4 原版资源集成

- 加入 libcimbar `bitmaps.h` 中的 20 张 Base91 内嵌 PNG。
- 移植原版 rlyeh Base91 变体（注意它不是标准 basE91 字母表）。
- `TileFactory` 在没有外部 PNG 时自动使用内嵌资源。
- 新增真实资源测试：20 张 PNG 均可解码；5×5、8×8 原版固定 Hash 值匹配；16 个 8×8 符号 Hash 均唯一，最小两两 Hamming Distance 为 18。


## 0.1.5 原版符号匹配器移植

- `SymbolMatcher` 按原版 `CimbDecoder::get_best_symbol()` 逐项移植：遍历 `ahash_result` 的原始顺序（中心 4、边 5/7/3/1、角 8/0/2/6），每个偏移依次遍历符号字典。
- Hamming Distance 使用 `Long.bitCount(candidateHash ^ tileHash)`；仅在距离严格变小时更新结果，因此平局保留先遇到的候选和字典符号。距离为 0 时立即返回。
- cooldown 与原版一致：跳过 drift index 等于 cooldown 的候选；cooldown 为 4 时改为 `0xFF`，确保中心候选不会被跳过。
- 原版实现没有设置最大距离门限，也没有依据次佳距离拒绝歧义结果；本版本不擅自添加这些规则。
- 新增 `match(FuzzyAHash, cooldown)` 入口，可直接接收 `FuzzyAHash` 的 FAST/ALL 结果；`match(BitMatrix, cellSize, cooldown)` 复现原版特殊 cooldown `0xFE` 时使用 ALL 九偏移的行为。

源码依据：[`CimbDecoder.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbDecoder.cpp) 的 `get_best_symbol()` / `decode_symbol()`，以及 [`ahash_result.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/image_hash/ahash_result.h) 的候选顺序。

## 0.1.6 CellDrift / CellPositions / AdjacentCellFinder

- `CellDrift`：移植原版九方向 drift 顺序、每轴 ±7 像素限幅，以及 `calculate_cooldown(previous, idx)` 规则。
- `CellPositions`：按原版 top / middle / bottom 三段顺序生成图块坐标，支持原版 chunk-major interleave、游标遍历和 reset。
- `AdjacentCellFinder`：按 anchor margin 布局查找 right / left / bottom / top 邻居，返回顺序与原版 `find()` 一致。
- 新增几何算法单元测试，覆盖边界行、跨区域邻接、drift 限幅、cooldown 和交织顺序。

源码依据：[`CellDrift.cpp/.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CellDrift.cpp)、[`CellPositions.cpp/.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CellPositions.cpp)、[`AdjacentCellFinder.cpp/.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/AdjacentCellFinder.cpp)、[`Interleave.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Interleave.h)。


### 0.1.7：FloodDecodePositions

- 新增 `geometry/FloodDecodePositions`，按原版优先级队列调度待解码图块。
- 初始化八个原版种子位置；已出队位置只处理一次。
- 使用错误距离作为优先级，较低距离优先。
- 根据 `AdjacentCellFinder` 扩展四邻域，并继承 drift/cooldown。
- 实现原版连续高置信度且 cooldown 为 4 时的横向/纵向 horizon 扩展。
- `reset()` 会清空 Java 队列和指令状态，避免原版 C++ reset 未清空容器导致的重复状态问题；正常首次初始化及解码调度规则保持一致。
- 队列耗尽时返回原版哨兵：index=0、坐标(0,0)、drift=(0,0)、cooldown=0xFF。


### 0.1.8：CimbDecoder / CimbReader 符号整合

- 新增 `decoder/CimbFrameConfig`，描述原版网格几何和符号配置，并提供默认 8x8 标准帧参数。
- 新增 `decoder/CimbDecoder`，整合原版 Bitmap 字典、AverageHash 与 SymbolMatcher，提供 bitmatrix 符号解码。
- 新增 `decoder/CimbReader`，实现 RGB 图像预处理、网格 padding、FloodDecodePositions 调度、带 drift 的图块裁切、符号识别和位置状态更新。
- `BitMatrix` 新增紧凑矩形 `crop()`，用于从整幅二值图中提取 (cellSize+2) 方形图块。
- 修正 `FloodDecodePositions` 的八个初始种子优先级：四个外角种子优先级为 0，四个内侧种子优先级为 1；同优先级保持入队顺序。
- 本阶段仅实现符号网格读取；透视变换、锚点检测、颜色解码、interleave 反向映射、ECC 和 Fountain 数据重组尚未实现。


## 0.1.9：颜色解码

本版本在符号识别之外，移植了 `CimbDecoder` 的颜色识别路径：

- `avgColor`：取图块内部区域均值，忽略最外侧一圈像素。
- `getBestColor`：按原版 `BEST_COLOR_FLOOR=48`、动态范围归一化和 `FIX_THRESH_HIGH=245` 规则修正 RGB。
- `colorDistance`：使用 `(R-G, G-B, B-R)` 相对颜色向量的平方距离，而不是 RGB 欧氏距离。
- 颜色表：沿用 `Common.cpp` 的 4 色、8 色、旧版和 encoder 专用调色板。
- `updateColorCorrection`：支持设置线程局部 3×3 RGB 矩阵；默认不启用颜色校正。
- `CimbReader.SymbolRead` 新增 `color()`，每次读取同时返回 symbol 与 color。

颜色读取坐标与原版 `CimbReader::read_color()` 一致：从已校正的图块坐标读取 `cellSize × cellSize` 区域，再由 `avgColor` 去掉一圈边缘。自动从定位锚点估计 von Kries 色彩适应矩阵，以及基于 fountain header 的完整 CCM 求解，尚未移植。


## 0.2.0：Interleave 与位流恢复

本版本新增：

- `stream/Interleave`：按原版 `Interleave.h` 实现 `interleaveIndices`、逆映射、列表交织和反交织。
- `stream/BitBuffer`：按原版 `bit_file/bitbuffer.h` 实现 MSB-first 位读写、跨字节读写和显式偏移写入。
- `stream/CellDataAssembler`：旧版 coupled 模式工具，将每个图块的 `symbol` 与 `color` 合并成单个 cell word；它不是现代 non-legacy decoder 的通用恢复流程。
- `CimbReader.readAll()` 与 `readDataBytes(...)`：提供旧版 coupled symbol+color 位流的兼容入口。非 legacy 标准帧请使用 0.2.2 新增的 `FrameDataDecoder.standard8x8()` 或自定义 `FrameDataDecoder` 双平面流程。

注意：本版本完成的是图块字段合并、交织位置映射和位缓冲层；尚未移植 Reed-Solomon 解码、Fountain Metadata 解析或 OpenRQ/Wirehair 解码。交织参数（块数、分区数）需要由对应帧配置明确传入，不能擅自假定。


## 0.2.1：Reed-Solomon 纠错

本版本新增纯 Java RS(255) 实现，参数与 libcimbar 的 libcorrect 包装器一致：GF(256)、primitive polynomial `0x187`、first consecutive root `1`、generator root gap `1`。

- `ecc/ReedSolomonCodec`：支持缩短 RS 码块、系统编码、Berlekamp-Massey 定位错误，以及 GF(256) 方程组求解错误幅值；最多纠正 `floor(parityBytes/2)` 个错误字节。
- `ecc/ReedSolomonStream`：按原版 `reed_solomon_stream` 的固定块输入/输出语义封装，解码失败时输出全零 BadChunk。
- 标准 8×8 配置：`eccBytes=30`、`eccBlockSize=155`，即每块 125 字节数据 + 30 字节校验。
- 新增原版 libcorrect 的 15/155 固定编码向量测试，以及 30/155 标准配置的 15 字节纠错测试。

兼容性说明：原版 `reed_solomon_streamTest.cpp` 的固定测试使用 `ecc=15, buffer_size=155`，其 140 字节输入和 15 字节校验向量已逐字节匹配；标准 8×8 参数则由 `GridConf.h` 指定为 30/155。本实现没有将这两组配置混为一谈。

源码依据：[`ReedSolomon.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/ReedSolomon.h)、[`reed_solomon_stream.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/reed_solomon_stream.h)、[`correct.h`](https://github.com/sz3/libcimbar/blob/master/src/third_party_lib/libcorrect/include/correct.h)、[`GridConf.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/GridConf.h)、[`reed_solomon_streamTest.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/test/reed_solomon_streamTest.cpp)。


## 0.2.2：Symbol / Color 双平面数据恢复

本版本将原版 `Decoder::do_decode()` 的非 legacy 两遍流程接入 Java：

1. `CimbReader.readSymbolNext()` 只读取符号值并保存索引、坐标和 drift 结果，不提前读取颜色。
2. `FrameDataDecoder` 按 `Interleave::interleave_reverse()` 将 symbol bits 写入独立的 `BitBuffer`，按固定 plane 容量补齐后执行独立 RS 解码。
3. Symbol plane 解码完成后调用可选 `Consumer<byte[]>` 回调，供后续 Fountain Header / 颜色校正逻辑初始化。
4. 然后按原始 cell index 顺序调用 `CimbReader.readColorAt()`，将 color bits 写入另一块独立的 `BitBuffer`，并独立执行 RS 解码。
5. 最终输出顺序为 `symbolData || colorData`，与原版两次 `symbolBuff.flush(rss)`、`colorBuff.flush(rss)` 写入同一输出流的顺序一致。

`CimbReader.readNext()` / `readAll()` 保留为兼容接口，但它们仍会即时读取颜色；需要原版两遍语义时使用 `FrameDataDecoder.decode(reader, callback)`。Fountain 帧则使用 `decodeFountain(reader, chunkSize, fountainBlocks, enableCcm, callback)`，由解码器在 symbol plane RS 解码后自动提取六字节 Metadata、计算 Header 颜色样本、更新 CCM，再开始 color plane 分类。

新增测试用原版 30/155 RS 块构造独立 symbol/color 编码平面，验证反交织位偏移、两次 RS 解码、输出拼接顺序，以及颜色读取必须发生在 symbol 回调之后。

源码依据：[`Decoder.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/Decoder.h)、[`CimbReader.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbReader.h)、[`CimbReader.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbReader.cpp)、[`bitbuffer.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/bit_file/bitbuffer.h)、[`Interleave.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Interleave.h)、[`reed_solomon_stream.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/reed_solomon_stream.h)。

## 0.2.3：Fountain Metadata 与颜色校正基础

本版本依据原版 `FountainMetadata.h` 与 `CimbReader.cpp` 新增：

- `fountain/FountainMetadata`：六字节 Fountain Header 的编码与解析。前四字节按原版小端 ID 解释；文件长度为 25 bit（最高位复用首字节 bit7）；block ID 为 16 bit 大端。支持广播 block ID 递增，并跳过不完整文件尾块对应的 radioactive block。
- `color/FountainColorCalibrator`：按原版容量、fountain block 数、交织参数和 metadata 长度计算 Header 颜色采样位置；按颜色类别聚合 RGB 均值，追加白点样本，执行 3×3 Moore-Penrose 最小二乘 CCM。
- `CimbReader`：提供 metadata 状态更新、cell RGB 采样，以及暗色/浅色模式白色锚点采样。

## 0.2.4：Fountain CCM 接入双平面解码

本版本将 0.2.3 的 Metadata 与 CCM 算法接入真实双平面流程：

1. `FrameDataDecoder.decodeFountain(...)` 先读取完整 Symbol 平面，并独立执行 RS 解码。
2. 使用 Symbol 平面解码后的前六字节更新 `FountainMetadata`，计算 radioactive block ID。
3. 按原版 `CimbReader::init_ccm()` 的容量、Header 间隔、交织坐标和 block ID 递增规则，生成颜色样本位置。
4. CCM 采样使用 `_positions.positions()` 对应的原始 cell 坐标（不使用 drift 修正坐标），采样 cell 内区 RGB；白点采样使用原版锚点区域。
5. 至少获得四种有效颜色类别时，计算 Moore-Penrose CCM 并写入 `CimbDecoder` 的线程局部校正矩阵。
6. 然后才开始读取 Color 平面，执行独立 RS 解码。

`decodeFountain` 参数由调用方提供 `chunkSize`、`fountainBlocks` 和 `enableCcm`。这与原版 `Config::fountain_chunk_size()`、`fountain_chunks_per_frame()` 的配置职责一致；当前工程尚未实现完整 Fountain 解码 Sink / Wirehair 数据重组，因此不会把这部分伪装成已完成。

新增测试覆盖原始 cell 坐标采样、重复 Header 区间的 block ID 递增，以及 CCM 在 Color 平面读取前安装。源码依据：[`Decoder.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/Decoder.h)、[`CimbReader.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbReader.cpp)、[`CimbDecoder.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbDecoder.cpp)、[`GridConf.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/GridConf.h)、[`Interleave.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Interleave.h)。

## 0.2.5：Fountain Sink 与 Wirehair 后端边界

本版本开始移植原版 `fountain_decoder_sink` / `fountain_decoder_stream` 的文件接收生命周期：

- `FountainDecoderStream`：按 `chunkSize` 拆分固定 Fountain packet；每个 packet 前 6 字节为 `FountainMetadata`，剩余字节作为 Wirehair symbol；按 block ID 去重；记录完成状态与进度。
- `FountainDecoderSink`：按 `encodeId & 0x7` 路由最多 8 个活动 stream；校验帧长度、文件长度和 stream slot 冲突；支持乱序 block、重复 block 丢弃、完成文件回调写出，以及无回调时手动 `recover(id)`。
- `FrameDataDecoder.decodeFountainToSink(...)`：将 Symbol plane 与 Color plane 分别 RS 解码后的 `symbolData || colorData` 按原版数据流顺序提交给 Fountain Sink。
- `FountainDecoderBackend`：定义 Wirehair decoder backend SPI，接口对应原版 `wirehair_decoder_create`、`wirehair_decode`、`wirehair_recover` 和 `wirehair_free` 的生命周期。

**重要边界：0.2.5 尚未包含 Wirehair 算法本身，也没有 JNI/native Wirehair adapter。** Sink 层已经可以通过可注入 backend 测试 packet routing、去重、完成和恢复流程，但尚不能独立完成真实 Wirehair 文件恢复。后续需要实现并用原版 Wirehair 测试向量验证纯 Java backend，或明确选择 JNI adapter；不能将测试用 Fake backend 当成 Wirehair 兼容实现。

源码依据：[`fountain_decoder_sink.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/fountain/fountain_decoder_sink.h)、[`fountain_decoder_stream.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/fountain/fountain_decoder_stream.h)、[`FountainDecoder.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/fountain/FountainDecoder.h)、[`FountainMetadata.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/fountain/FountainMetadata.h)、[`Decoder.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/Decoder.h)。


## 0.2.6：Wirehair GF(256 算术基础

本版本开始纯 Java Wirehair 移植，新增 `fountain/wirehair/GF256`：

- 按 Wirehair 上游 `WirehairCodec.h` 文档使用 GF(256) 不可约多项式 `0x14D`（`x^8 + x^6 + x^3 + x^2 + 1`），生成元为 `2`。注意这与本工程 Reed-Solomon 使用的 `0x187` 是两个不同的有限域参数，不能混用。
- 实现加/减（XOR）、乘法、除法、乘法逆元、整数幂。
- 实现 byte[] 标量乘、`destination ^= coefficient * source`、多行线性组合，为 Wirehair heavy matrix 和 GF256 row solver 提供底层操作。
- 静态初始化检查生成元是否覆盖全部 255 个非零元素。
- 新增有限域遍历、逆元、分配律、向量运算和线性组合测试。

**完成边界：** 0.2.6 只完成 Wirehair 的 GF(256) 算术层，尚未实现 Wirehair 的 PRNG、Peel Row 参数、矩阵构造、Peeling、Compression、Gaussian Elimination、编码器或解码器。`FountainDecoderBackend` 仍然需要后续真正的 Wirehair 后端实现。当前没有声称已具备 Wirehair 互通能力。

源码依据：[`WirehairCodec.h`](https://github.com/catid/wirehair/blob/master/WirehairCodec.h) 中的 Heavy Submatrix / GF(256) 说明；Wirehair 上游 README 对编码/解码 API 生命周期的说明：[`README.md`](https://github.com/catid/wirehair/blob/master/README.md)。

## 0.2.7：Wirehair PCG 与 Peel/Mix 行列生成

本版本在 0.2.6 的 GF(256) 算术层之上，移植 Wirehair 稀疏矩阵前端的确定性行生成基础：

- `PCGRandom`：Wirehair `PCGRandom::Seed/Next` 的 64-bit LCG + PCG XSH-RR 32-bit 输出；Java `long` 溢出按 C++ `uint64_t` 模 2^64 行为处理。
- `PeelRowParameters`：原版 64 项 `kPeelCountDistribution`、weight-1 的 1/128 概率门限、`GeneratePeelRowWeight()` 和 `PeelRowParameters::Initialize()`。
- `PeelRowIterator`：原版 Weyl 列迭代与非素数列数时的回绕修正。
- `RowMixIterator`：原版 M 矩阵每行固定三个列索引的生成。

**当前边界：** 本版本只生成 P/M 行的参数与列索引；尚未实现 Wirehair 的完整编码矩阵 P/M/D/J/H、dense seed 表、Peeling solver、Compression、Gaussian Elimination、Substitution，以及 encoder/decoder。因此目前不能宣称与 Wirehair 编解码互通。

### 0.2.7 验证

- 主源码使用 `javac --release 17` 编译通过。
- PCG 使用标准参考序列校验通过。
- Peel weight 边界及 10,000 组确定性行参数/列唯一性独立 harness 校验通过。
- JUnit 测试源码已添加，但当前环境未安装 Maven，未执行 `mvn clean test`。

### 0.2.7 原版算法交叉核对补充

- 使用独立 C++ 参考程序按 Wirehair `PCGRandom`、`GeneratePeelRowWeight` 和 `PeelRowParameters::Initialize` 计算固定输入 `row_seed=0x12345678, p_seed=0x10203040, N=257, mix_count=31`，得到 `PeelRowParameters(2, 240, 250, 25, 26)`；Java 输出完全一致。
- 另对 N=2..2048 的 614,100 组行参数进行列索引范围、无重复和数量检查，全部通过。

## 0.2.8：面向应用的解码 API

新增 `com.hrz.libcimbar.api`：

- `CimbarDecoderConfig`：统一帧几何、颜色模式、RS 和 interleave 参数。
- `CimbarDecoder`：封装 `CimbDecoder`、`CimbReader` 和 `FrameDataDecoder`。
- `CimbarFrameResult`：以防御性复制方式返回 symbol plane、color plane 和合并数据。
- 支持普通单帧解码、带 CCM 的 Fountain 帧解码，以及提交到 `FountainDecoderSink`。

示例：

```java
CimbFrameConfig frame = CimbFrameConfig.standard8x8();
CimbarDecoder decoder = new CimbarDecoder(CimbarDecoderConfig.standard8x8(frame));
BufferedImage image = ImageIO.read(new File("frame.png"));
CimbarFrameResult result = decoder.decodeFrame(image);
byte[] payload = result.data();
```

边界说明：该 API 输入必须是已经定位、裁切并几何归一化的 CIMBAR 帧；本版本没有实现任意照片中的码区定位/透视矫正，也没有实现帧编码器或完整 Wirehair backend。`standard8x8` 仅提供既有 FrameDataDecoder 的参数组合，不代表所有输入帧尺寸均已验证。

## 0.2.9：CIMBAR Tile Encoder（第一阶段）

本版本新增 `com.hrz.libcimbar.encoder.CimbEncoder`，按 libcimbar 原版 `src/lib/cimb_translator/CimbEncoder.cpp` 的索引规则，将一个 cell packed word 映射到原版 Symbol bitmap 与颜色组合：

- `symbol = index % (1 << symbolBits)`
- `color = index / (1 << symbolBits)`
- `index = packedBits % (numSymbols * numColors)`（Java 使用等价的低位掩码，因为 tile 总数为 2 的幂）
- 通过已有 `TileFactory` 和内嵌原版 bitmap 资源生成 tile。

示例：

```java
CimbEncoder encoder = new CimbEncoder(4, 2, true, 1);
BufferedImage tile = encoder.encode(0b10_1011);
CimbEncoder.Tile detail = encoder.encodeTile(0b10_1011);
// detail.symbol() == 11, detail.color() == 2
```

**重要边界：0.2.9 只实现原版 `CimbEncoder` 的 Tile 映射层，不是完整 `Encoder`，也不生成可被原版接收端识别的完整 CIMBAR 帧。** 尚未接入 CimbWriter 的 anchors/guides、完整 CellPositions 图像布局、RS stream、symbol/color 双遍 bit-plane 编排及 Fountain/Wirehair。不要将单独 tile 当作完整视觉码。

## 0.3.1：CIMBAR Frame Writer 图像布局（第一阶段）

本版本在 0.2.9 的 Tile Encoder 之上新增：

- `CimbFrameMarkers`：接收原版 Anchor、Secondary Anchor、Horizontal Guide、Vertical Guide 图像，并在构造时复制资源，避免调用方修改内部图案。
- `CimbFrameWriter`：按 libcimbar `CimbWriter.cpp` 的坐标规则绘制四角定位图案、横纵 Guide，并按 `CellPositions` 的原版顺序写入 Tile。
- 标准 8×8 布局使用 1024×1024 内部图像、112×112 cell grid、9 像素间距、8 像素偏移、四角各 6×6 cell 留空、155 个 RS block interleave、2 个 partitions，数据 cell 总数为 12,400。
- 支持指定更大的输出画布，内部 1024×1024 帧按原版逻辑居中。
- `writeNext(int)` 按顺序写一个 packed cell；`writeAll(int[])` 批量写入；`image()` 返回图像副本。

示例（Marker PNG 必须使用 libcimbar 对应模式的原版资源）：

```java
BufferedImage anchor = ImageIO.read(new File("bitmap/anchor-dark.png"));
BufferedImage secondary = ImageIO.read(new File("bitmap/anchor-secondary-dark.png"));
BufferedImage horizontal = ImageIO.read(new File("bitmap/guide-horizontal-dark.png"));
BufferedImage vertical = ImageIO.read(new File("bitmap/guide-vertical-dark.png"));
CimbFrameMarkers markers = new CimbFrameMarkers(anchor, secondary, horizontal, vertical);
CimbEncoder encoder = new CimbEncoder(4, 2, true, 1);
CimbFrameWriter writer = new CimbFrameWriter(CimbFrameConfig.standard8x8(), encoder, markers);
writer.writeAll(packedCellValues);
BufferedImage frame = writer.image();
```

**重要边界：** 本版本尚未把四种 Anchor/Guide PNG 资源内嵌到 JAR；调用方必须提供与当前 libcimbar 版本、dark/light 模式匹配的原版资源。测试使用合成 marker 只验证绘制位置，不代表这些测试图案能被 libcimbar 识别。此外，当前 `writeAll` 接收的是已经完成 bit-plane/ECC 处理的 packed cell 值；尚未实现原版完整 `Encoder` 的 RS stream、Symbol/Color 双平面字节编排、从原始 byte[] 到 cell 序列的转换，以及 Fountain/Wirehair 帧生成。因此本版本可以生成布局完整的测试图像，但不能声称已经具备原版文件编码互通能力。

源码依据：[`CimbWriter.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbWriter.cpp)、[`Config.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Config.h)、[`GridConf.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/GridConf.h)、[`CellPositions.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CellPositions.cpp)、[`Interleave.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Interleave.h)。

### 0.3.1 验证

- 主源码使用 `javac --release 17` 编译通过。
- 独立 harness 验证标准布局 12,400 个 cell、interleave 后固定坐标、Anchor/Guide 绘制位置、Tile 绘制位置、容量边界，全部通过。
- 新增 JUnit 测试源码覆盖标准网格、marker/tile 放置及越界校验；当前环境未安装 Maven，未执行 `mvn clean test`。

## 0.3.1：原始字节到 CIMBAR Cell 数据编排

新增 `com.hrz.libcimbar.encoder.CimbarFrameDataEncoder`，移植当前 libcimbar `src/lib/encoder/Encoder.h` 中非 coupled 模式的 `Encoder::encode_next()` 数据准备流程：

- 根据帧几何计算数据 cell 数量，并分别计算 Symbol Plane 与 Color Plane 的字节容量。
- 按 RS payload 大小将输入补零到单帧容量，使用既有 `ReedSolomonStream` 逐块生成 RS 编码数据。
- 第一遍将 RS 字节流按 `symbolBits` 读取，写入每个 cell word 的低位 Symbol 区域。
- 第二遍从后续 RS 字节流按 `colorBits` 读取，写入每个 cell word 的高位 Color 区域。
- 输出 `int[] packedCells`，可直接传给 `CimbFrameWriter.writeAll()`；Writer 自己按原版 `CellPositions` 的 interleave 顺序将 cell 放到图像位置。
- `Result` 提供原始长度、补零后的 payload、RS 字节流和两个 plane 的长度，所有数组访问均为防御性复制。

标准 8×8 / Mode B 参数下：12,400 cells；Symbol Plane 为 6,200 bytes（40 个 RS(155,125) codeword）；Color Plane 为 3,100 bytes（20 个 RS(155,125) codeword）；单帧输入容量为 7,500 bytes。该容量由当前 `GridConf.h` 的 4 symbol bits、2 color bits、30/155 RS 和 12,400 cells 推导。

用法：

```java
CimbFrameConfig config = CimbFrameConfig.standard8x8();
CimbarFrameDataEncoder dataEncoder = new CimbarFrameDataEncoder(config);
CimbarFrameDataEncoder.Result encoded = dataEncoder.encode(fileBytes);
CimbEncoder tileEncoder = new CimbEncoder(config.symbolBits(), Integer.numberOfTrailingZeros(config.numColors()), config.dark(), 1);
CimbFrameWriter writer = new CimbFrameWriter(config, tileEncoder, originalMarkers);
writer.writeAll(encoded.packedCells());
BufferedImage frame = writer.image();
```

**重要边界：** 本版本完成的是原版非 coupled `Encoder::encode_next()` 的 RS + 双平面 bit 编排前半段，并能输出给 Frame Writer 使用的 packed cell 数组。Marker PNG 仍由调用方提供；Fountain/Wirehair 数据源、文件名 header/zstd 文件协议、coupled/legacy 编码模式、任意模式的原版互通验证尚未完成。没有使用原版接收端对 Java 输出帧进行端到端验证，因此不能宣称已经实现完整文件编码互通。

源码依据：[`Encoder.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/Encoder.h)（`encode_next()`，symbol pass / color pass / RS stream）、[`reed_solomon_stream.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/encoder/reed_solomon_stream.h)、[`GridConf.h`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/GridConf.h)、[`CimbWriter.cpp`](https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/CimbWriter.cpp)。

### 0.3.1 验证

- 主源码使用 `javac --release 17` 编译通过。
- 独立 Java harness 验证标准 8×8 容量、12,400 个 packed cells、RS 输出长度 9,300 bytes，以及 Symbol/Color 两个 plane 分别 RS 解码后恢复原始 5,000 + 2,500 bytes，全部通过。
- 新增 JUnit 测试源码覆盖标准容量、双 plane RS 回环、短输入补零和超容量拒绝；仅用本地 JUnit API stub 编译新增测试源码，未实际运行 JUnit。
- 当前环境没有 Maven，未执行 `mvn clean test`。


## 0.3.2：统一单帧编码入口

新增 `encoder/CimbarEncoder`，将 `CimbarFrameDataEncoder`、`CimbEncoder` 和 `CimbFrameWriter` 串接为单帧编码 API：

```java
CimbFrameConfig config = CimbFrameConfig.standard8x8();
CimbFrameMarkers markers = new CimbFrameMarkers(anchor, secondaryAnchor, horizontalGuide, verticalGuide);
CimbarEncoder encoder = new CimbarEncoder(config, markers);
BufferedImage frame = encoder.encodeFrame(fileBytes);
```

如需查看原始长度、补零量和 RS 中间数据，可调用 `encodeDetailed(byte[])`，返回 `CimbarEncoder.Result`。标准 8×8 配置每帧 12,400 个数据 cell，当前固定帧数据输入容量为 7,500 字节；短输入会补零，超出容量会抛出异常。

**Marker 资源边界：** 本版本仍要求调用方传入四张原版 marker 图片。没有将合成图片伪装成原版资源，也没有声称已完成原版 PNG 的内嵌。`CimbFrameWriter` 的 marker 摆放坐标已对照当前上游 `CimbWriter.cpp`；原版资源的可靠提取与内嵌留待后续完成。

**兼容性边界：** 该入口只完成单帧的 RS 双平面数据编排和图像绘制，不包含文件级 Wirehair Fountain、Zstd 压缩/文件头，也未用官方 libcimbar decoder 做端到端互通验证。
