# libcimbar-java 0.1.0-SNAPSHOT

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
- `AverageHash`：平均哈希基础实现。
- `TileFactory`：按 classpath 原版 PNG 资源着色。
- `SymbolDictionary`：生成符号 Hash 字典。
- `SymbolMatcher`：按候选顺序进行汉明距离匹配。

## 原版 bitmap 资源

`TileFactory` 需要原版资源。请从 https://github.com/sz3/libcimbar/tree/master/bitmap 下载对应 PNG，放入：

```text
src/main/resources/cimbar/bitmap/2/00.png ... 03.png
src/main/resources/cimbar/bitmap/4/00.png ... 0f.png
```

目前压缩包不伪造原版图案，也不内置自行绘制的替代图案。未放入资源时，基础 Demo 和单元测试仍可运行；调用 `SymbolDictionary.load(...)` 会明确提示缺少资源。

## 重要兼容性说明

这不是完整 libcimbar Java 移植，也不是可直接解码 libcimbar 图片的最终版本。当前工程是可构建的算法基础工程。以下部分尚未完成或尚未与原版逐像素/逐字节验证：

1. OpenCV `cvtColor`、`filter2D`、`adaptiveThreshold` 的所有边界和舍入细节。
2. OpenCV `resize(INTER_LINEAR)` 的所有优化路径与像素级一致性。
3. 原版 `bitmaps.h` Base91 内嵌资源与 PNG 资源的一致性核验。
4. 完整的 `CimbReader` 定位、透视变换、drift 跟踪、FloodDecodePositions。
5. 颜色校正、颜色匹配、交织、Reed-Solomon 解码及完整帧协议。
6. libcimbar 编码器和文件层 Wirehair/Zstd 协议。

因此，现阶段不要将它作为完整解码器接入 QOcr 的正式传输流程。建议先补齐原版 PNG，再用原版测试图和固定输出做逐项回归。

## Maven 集成（后续）

当 API 稳定后，可在 QOcr 的 `pom.xml` 中添加：

```xml
<dependency>
  <groupId>com.hrz</groupId>
  <artifactId>libcimbar-java</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

本地安装：

```bash
mvn clean install
```
