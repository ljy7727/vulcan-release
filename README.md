# 硫化工艺助手 · 桌面版更新分发

桌面版（Windows）源码与更新清单。**仓库不含任何业务数据**——工艺标准、拉直高度、不良简称、判胎分析等
数据由使用者本地导入（Excel），不随代码分发。

## 目录

| 路径 | 说明 |
|------|------|
| `manifest.json` | 版本清单，客户端据此检查更新 |
| `desktop/` | 桌面版源码（Kotlin + Compose for Desktop）与构建配置 |

## 构建

需 JDK 17 + Gradle 8.9，**必须在不含中文的路径下构建**（中文路径会导致测试进程类加载失败）。

```bash
cd desktop
gradle test -PkulcanDataDir="<你的 Excel 数据目录>"
gradle createDistributable     # 产出 app-image
```

## 更新流程

1. 修改 `desktop/src` 后提升 `AppVersion.VERSION`
2. 构建并打包为 zip，上传到 Releases
3. 更新根目录 `manifest.json`（版本号、下载地址、sha256）
4. 客户端「更新」页点检查更新即可下载

## 激活

新版本首次启动需输入序列号（本地校验，离线可用）。激活状态记录在本机 `prefs.json` 的
`activated_version`，版本变化后需重新激活。

## 免责声明

本工具为辅助查询工具，工艺与质量判定须以技术部门正式下发的工艺文件为准。
