# libcimbar-java 0.4.0

面向 Java 17 的纯 Java CIMBAR 编码/解码移植工程。0.4.0 在原有 CIMBAR 单帧图像管线之上增加了可直接使用的**单帧 UTF-8 文字**和**单帧文件**应用接口。

## 重要的兼容性边界

- `encodeText` / `encodeFile` 使用本工程定义的 `CMBJ` 单帧应用封装（版本 1：类型、长度、UTF-8 文件名、SHA-256、内容）。它不是 libcimbar 官方 Wirehair/Zstd Fountain 文件协议，因此不能据此宣称与官方 `cimbar` 文件流互通。
- CIMBAR 的图像数据层仍使用本工程移植的 Symbol/Color 双平面、RS(155,125) 与原版 tile 字典。官方 `Encoder::encode_next()` 的当前实现按 Symbol pass、Color pass 写入帧；官方文件发送则另走 Wirehair + Zstd Fountain 流。参考上游 `Encoder.h`、`Decoder.h`、`GridConf.h` 与 `DETAILS.md`。
- 当前高层 decoder 输入必须是**已裁切、几何归一化的 CIMBAR 帧**；本版本不负责从任意相机照片中定位码图或透视矫正。
- 标准 8×8 配置单帧两平面 RS 输入总容量为 7,500 字节。应用头为 44 字节，文件还需扣除 UTF-8 文件名字节数；因此实际内容容量小于 7,500 字节。超限会在编码前拒绝。
- 原版 marker PNG 尚未内嵌在本版本包中。生成完整帧时请提供 libcimbar 上游 `bitmap/` 目录；不使用伪造 marker 冒充原版资源。默认构造器会从 classpath `/cimbar/bitmap` 加载，资源缺失时会抛出 IOException。

## 环境与构建

- JDK 17+
- Maven 3.8+
- 不需要 JNI、OpenCV、ZXing 或本地动态库

```bash
mvn clean test
mvn package
```

## 单帧文字发送/接收

```java
CimbFrameConfig frameConfig = CimbFrameConfig.standard8x8();
CimbarEncoder encoder = new CimbarEncoder(frameConfig, Path.of("/path/to/libcimbar/bitmap"));
BufferedImage frame = encoder.encodeText("你好，QOcr！");

CimbarDecoder decoder = new CimbarDecoder(CimbarDecoderConfig.standard8x8(frameConfig));
CimbarTextResult received = decoder.decodeText(frame);
System.out.println(received.text());
System.out.println(received.sha256());
```

## 单帧文件发送/接收

```java
BufferedImage frame = encoder.encodeFile(Path.of("sample.zip"));
CimbarFileResult received = decoder.decodeFile(frame);
Path saved = received.saveTo(Path.of("received"));
System.out.println(saved);
System.out.println(received.sha256());
```

`saveTo` 会拒绝带目录分隔符的文件名，并使用 `CREATE_NEW`，不会覆盖目标目录中已经存在的文件。

## API

- `CimbarEncoder.encodeText(String)`：UTF-8 文字单帧编码。
- `CimbarEncoder.encodeFile(Path)`：文件名 + 原始字节单帧编码。
- `CimbarEncoder.encodeFrame(byte[])`：底层原始数据帧编码，不附加 `CMBJ` 应用头。
- `CimbarDecoder.decodeText(BufferedImage)`：解码、校验并返回文字。
- `CimbarDecoder.decodeFile(BufferedImage)`：解码、校验并返回文件结果。
- `CimbarFileResult.saveTo(Path)`：安全地保存接收文件。
- `inputCapacityBytes()`：RS 编码前的原始两平面容量；不是最终文件内容容量。

## 后续互通工作

0.4.0 完成的是 Java 端单帧应用闭环。若要与官方 `libcimbar` 文件发送器/接收器互通，下一阶段必须移植官方 `fountain_encoder_stream` / `fountain_decoder_sink`、Wirehair packet 流与 Zstd 文件头/压缩格式；不能用本工程的 `CMBJ` 封装替代官方 Fountain 协议。

## License

本工程移植的 libcimbar 相关代码遵循上游项目 MPL-2.0；请同时阅读 `THIRD_PARTY_NOTICES.md` 和上游许可证。
