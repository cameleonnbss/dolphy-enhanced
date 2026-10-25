# Dolphy Enhanced — 中文

[Dolphy](https://github.com/unvoiddd/Dolphy-App) 的增强分支 —— 一个用于无线协议
研究、NFC/RFID 与硬件模拟的 Android 多功能工具。大多数模块无需 root。

**由 [camzzz](https://github.com/camzzz) 增强** · 原始项目来自
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3。

> ⚠️ 只可在你拥有或明确获得测试授权的设备上使用。干扰、阻断或截获他人设备在
> 大多数国家属于违法，**不属于**本版本范围。首次启动会出现同意页。

## 本分支新增内容

- **15 种语言**，接线正确：`values/` 现在存放英文，因此未完全翻译的语言会回退
  到英文而不是俄文。语言选择器带搜索、显示本地名称、支持 RTL（阿拉伯语），
  以及“跟随系统”模式。
- **不再会让手机崩溃的词表**：内置包逐行流式读取，导入的列表只规范化一次并
  写入文件（不再往 SharedPreferences 塞 JSON 大块），以及**生成模式**——用掩码
  产出数十万候选，APK 体积零增加。
- **卡片数据库**：37 类 NFC/RFID 家族（MIFARE、DESFire、NTAG、FeliCa、
  Calypso、EMV AID、HID、LEGIC、交通卡、电子护照……），可按名称、ATQA、SAK
  或 AID 搜索。
- **Root 检测**：是否已 root、安装的是哪个管理器（Magisk / KernelSU /
  APatch / SuperSU）、`su` 路径与版本，带缓存与超时。
- **首次启动同意页**：仅限教育与授权目标，可在 设置 → 关于 中重新阅读。
- **功能页搜索**、新的自适应图标、应用名“Dolphy Enhanced”。

## 编译

```bash
gradlew.bat assembleDebug     # Windows
./gradlew assembleDebug       # Linux / macOS
```

需要 JDK 17+ 与 Android SDK 36。APK 位于 `app/build/outputs/apk/debug/`。

## 文档

- [`docs/ENHANCED.md`](docs/ENHANCED.md) —— 更新记录与路线图。
- [`docs/MODULE-ROADMAP.md`](docs/MODULE-ROADMAP.md) —— 计划做什么，以及明确
  不做什么。
- [`docs/ADD-A-MODULE.md`](docs/ADD-A-MODULE.md) —— 如何扩展应用。

## 许可证

GNU GPL v3，见 [`LICENSE`](LICENSE)。保留对上游项目的署名：这是许可证要求，
不是礼节。

[← 返回英文 README](README.md)
