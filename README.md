# CardCase (AegisDocs)

该项目是一款基于安卓（Java 开发）的证件扫描管理应用，支持身份证、银行卡等证件的扫描，且可按需启用光学字符识别（OCR）功能。

## 本地部署

1. 在 Android Studio 中打开该项目。
2. 按常规操作确保 local.properties 文件中已配置好 sdk.dir 路径。
3. 配置腾讯 OCR 接口凭证（仅课程作业用途）

将以下密钥添加至 `local.properties`文件中（请勿提交该文件）：
```
TC_SECRET_ID=...
TC_SECRET_KEY=...
TC_REGION=ap-guangzhou   # optional
```

你也可将这些密钥以相同的名称配置为环境变量。

## 安全说明

请勿将任何真实的云服务 SecretId/SecretKey（密钥 ID / 密钥值）提交至 Git 版本记录中。
生产环境下，应通过后端服务生成短期有效凭证 / 令牌，而非将长期有效密钥打包至 APK 安装包内。

