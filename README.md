# libcimbar-java 0.1.4-SNAPSHOT

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
- `SymbolMatcher`：按候选顺序进行汉明距离匹配。

## 原版 bitmap 资源

工程已经内置 libcimbar `src/lib/cimb_translator/bitmaps.h` 中的 20 张符号 PNG。资源以原版自定义 Base91 编码文本保存在 `src/main/resources/cimbar/original-bitmaps-base91.txt`，运行时由 `Base91` 解码并通过 JDK `ImageIO` 读取，不需要手工下载图片，也不需要 OpenCV/stb_image。

`TileFactory` 仍支持外部 classpath PNG 覆盖内嵌版本，便于后续替换和对照。内嵌资源覆盖 2-bit 的 `00..03` 和 4-bit 的 `00..0f`。

## 重要兼容性说明

这不是完整 libcimbar Java 移植，也不是可直接解码 libcimbar 图片的最终版本。当前工程是可构建的算法基础工程。以下部分尚未完成或尚未与原版逐像素/逐字节验证：

1. OpenCV `cvtColor`、`filter2D`、`adaptiveThreshold` 的所有边界和舍入细节。
2. OpenCV `resize(INTER_LINEAR)` 的所有优化路径与像素级一致性。
3. 原版 PNG 的像素级颜色/透明度边界核验，以及所有 symbol/color mode 的完整兼容验证。
4. 完整的 `CimbReader` 定位、透视变换、drift 跟踪、FloodDecodePositions。
5. 颜色校正、颜色匹配、交织、Reed-Solomon 解码及完整帧协议。
6. libcimbar 编码器和文件层 Wirehair/Zstd 协议。

因此，现阶段不要将它作为完整解码器接入 QOcr 的正式传输流程。当前 `FuzzyAHash` 已按原版 `ahash_result`、`bit_extractor` 和 `fuzzy_ahash(bitmatrix)` 的规则实现；合成网格测试验证了九个偏移位置与 FAST 顺序。`AverageHashOriginalVectorTest` 保留了原版测试向量；`OriginalBitmapAssetsTest` 进一步直接加载内嵌的真实 PNG，并对原版测试中的 8 个固定 Hash 值做了真实资源回归验证，同时检查 16 个 8×8 符号的最小两两 Hamming Distance 为 18。原版测试源文件：https://github.com/sz3/libcimbar/blob/master/src/lib/image_hash/test/averageHashTest.cpp。原版 `getTile` 的颜色和背景逻辑见：https://github.com/sz3/libcimbar/blob/master/src/lib/cimb_translator/Common.cpp。仍需补齐真实 bitmap，再验证 `TileFactory`、`AverageHash` 和 `SymbolDictionary` 的端到端输出。

## Maven 集成（后续）

当 API 稳定后，可在 QOcr 的 `pom.xml` 中添加：

```xml
<dependency>
  <groupId>com.hrz</groupId>
  <artifactId>libcimbar-java</artifactId>
  <version>0.1.4-SNAPSHOT</version>
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
