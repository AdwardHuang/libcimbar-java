# libcimbar-java 0.1.9-SNAPSHOT

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
5. 自动锚点白色采样与 von Kries 色彩适应、基于 Fountain Header 的完整 CCM 求解、交织反向映射、Reed-Solomon 解码及完整帧协议。
6. libcimbar 编码器和文件层 Wirehair/Zstd 协议。

因此，现阶段不要将它作为完整解码器接入 QOcr 的正式传输流程。当前颜色分类核心已移植，但尚未完成自动色彩校正；`FuzzyAHash` 已按原版 `ahash_result`、`bit_extractor` 和 `fuzzy_ahash(bitmatrix)` 的规则实现；合成网格测试验证了九个偏移位置与 FAST 顺序。`AverageHashOriginalVectorTest` 保留了原版测试向量；`OriginalBitmapAssetsTest` 进一步直接加载内嵌的真实 PNG，并对原版测试中的 8 个固定 Hash 值做了真实资源回归验证，同时检查 16 个 8×8 符号的最小两两 Hamming Distance 为 18。原版测试源文件：https://github.com/sz3/libcimbar/blob/master/src/lib/image_hash/test/averageHashTest.cpp。原版 `getTile` 的颜色和背景逻辑见：https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Common.cpp。当前已内置真实 bitmap，并通过固定 Hash 向量验证资源、`AverageHash` 和 `SymbolDictionary` 的基础输出；OpenCV 预处理的逐像素一致性仍需继续验证。

## Maven 集成（后续）

当 API 稳定后，可在 QOcr 的 `pom.xml` 中添加：

```xml
<dependency>
  <groupId>com.hrz</groupId>
  <artifactId>libcimbar-java</artifactId>
  <version>0.1.9-SNAPSHOT</version>
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
