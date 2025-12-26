# CardCase (AegisDocs)

该项目是一款基于安卓（Java 开发）的证件扫描管理应用，支持身份证、银行卡等证件的扫描，有OCR信息识别功能，扫描图片（可导出PDF）加密存储。

## 本地部署

1. 在 Android Studio 中打开该项目。
2. 按常规操作确保 local.properties 文件中已配置好 sdk.dir 路径。
3. 配置腾讯 OCR 接口凭证（仅课程作业用途）

将以下密钥添加至 `local.properties`文件中（请勿提交该文件）：
```
TC_SECRET_ID="your SECRET ID"
TC_SECRET_KEY="your SECRET KEY"
TC_REGION=ap-guangzhou   # optional
```
你也可将这些密钥以相同的名称配置为环境变量。


## 重要目录/文件说明：
App.java：Application 入口；加载 SQLCipher native 库，并初始化默认证件类型数据。

---
core.auth.BiometricGate.java：生物识别“门禁”封装（用于进入敏感页面/查看敏感数据前做验证）。

core.filestore.EncryptedFileStore.java：加密文件存储（把图片/PDF 按相对路径加密落盘，并支持读取）。

---
core.network：（占位，由于完成课程作业需要暂时未配置后端）

BackendApi.java：后端 API 接口定义（Retrofit 风格；如暂未接入可视为预留）。

BackendClient.java：Retrofit 客户端创建/配置（baseUrl、拦截器等）。

BackendConfig.java：后端配置（服务器地址/超时等，通常可统一管理）。

---
core.network.dto：

IdCardOcrResponse.java：身份证 OCR 结果 DTO/模型。

TencentBankCardOcrResponse.java：腾讯银行卡 OCR 响应 DTO（Gson 反序列化用）。

TencentIdCardOcrResponse.java：腾讯身份证 OCR 响应 DTO（Gson 反序列化用）。

---
core.network.tencent

TencentSecrets.java：腾讯 OCR 密钥读取入口（从 BuildConfig/环境配置读取，避免硬编码）。

TencentCloudV3Signer.java：TC3-HMAC-SHA256 签名实现（生成 Authorization 请求头）。

TencentOcrIdCardClient.java：身份证 OCR 的 OkHttp 调用封装（构造请求、签名、解析响应）。

TencentOcrBankCardClient.java：银行卡 OCR 的 OkHttp 调用封装（同上，Action/字段不同）。

TencentAdvancedInfoParser.java：解析 OCR 的 AdvancedInfo（如裁剪图 base64 等）。

---
core.pdf.IdCardPdfService.java:身份证 PDF 生成服务（把正反面合成为 PDF）。

---
core.security

KeyMaterial.java：加密材料/密钥派生与管理（提供 EncryptedFileStore、SQLCipher 等使用的密钥来源）。

SecurePrefs.java：安全偏好存储封装（保存少量敏感配置/状态）。

---
core.util.JsonUtils.java：JSON 工具（安全 parse、取字段、空值判断等，避免重复样板代码）。

---
core.scan
ScanSpec.java：扫描“规格/插件接口”；身份证/银行卡通过实现它描述差异逻辑。

ScanPipeline.java：通用管线执行器；统一 orchestrate：校验密钥→取/建 item→调用 spec→保存 blob→更新 item。

ScanSessionParams.java：扫描会话参数（itemId、mode 等输入参数的统一载体）。

ScanCaptureStep.java：拍摄步骤定义（一步/两步；FRONT/BACK 等）。

ScanOutcome.java：Spec 执行后的产物（新状态、infoJson、要落盘/落库的 blobs 列表）。

BlobToSave.java：单个 blob 的保存描述（slot、relPath、mime、bytes、blobRefId 等）。

---
feature.documents

MainActivity.java：主界面容器（承载 DocumentListFragment）。

DocumentListFragment.java：证件类型入口列表（创建/进入身份证、银行卡等模块）。

DocumentTypeAdapter.java：证件类型 RecyclerView 适配器（渲染类型卡片/列表项）。

---
feature.idcard

dCardListActivity.java：身份证条目列表页。

IdCardListAdapter.java：身份证条目列表适配器。

IdCardDetailActivity.java：身份证详情页（Tab + ViewPager2，含“扫描”菜单入口）。

IdCardPagerAdapter.java：身份证详情页的 Pager 适配器（信息/图片/文件 Tab）。

---
feature.idcard.tabs

IdCardInfoFragment.java：身份证信息展示（从 infoJson 显示字段）。

IdCardImagesFragment.java：身份证图片展示（读取 FRONT/BACK blob）。

IdCardFilesFragment.java：身份证文件展示（PDF 等 blob）。

---
feature.idcard.scan

IdCardScanActivity.java：身份证扫描入口 Activity（很薄：提供布局 + IdCardScanSpec + params）。

IdCardScanSpec.java：身份证扫描差异实现（两步拍摄、OCR、裁剪图提取、PDF 生成、落库字段等）。

ImageProxyBytes.java：ImageProxy → JPEG bytes 工具（拍照回调中复用）。

YuvToJpegConverter.java：YUV 转 JPEG 的底层转换工具（被 ImageProxyBytes 调用）。

ScanOverlayView.java：扫描取景框/引导动画 Overlay（提示用户对齐证件）。

---
feature.bankcard

BankCardListActivity.java：银行卡条目列表页。

BankCardListAdapter.java：银行卡条目列表适配器。

BankCardDetailActivity.java：银行卡详情页（含底部弹窗选择“扫正面OCR/只拍背面”）。

BankCardPagerAdapter.java：银行卡详情页 Pager 适配器（信息/图片 Tab）。

---
feature.bankcard.tabs

BankCardInfoFragment.java：银行卡信息展示（卡号、银行名等）。

BankCardImagesFragment.java：银行卡图片展示（FRONT/BACK blob）。

---
feature.bankcard.scan

BankCardScanActivity.java：银行卡扫描入口 Activity（很薄：提供布局 + BankCardScanSpec + params）。

BankCardScanSpec.java：银行卡扫描差异实现（一步拍摄；根据 mode 决定 OCR/只存背面等逻辑）。

---
feature.settings

SettingsActivity.java：设置页（通常用于安全选项、生物识别开关、说明等）。

---
feature.scan

BaseScanActivity.java：通用 CameraX 扫描 UI；负责权限/相机初始化/多步拍摄/触发 ScanPipeline。



## 安全说明

请勿将任何真实的云服务 SecretId/SecretKey（密钥 ID / 密钥值）提交至 Git 版本记录中。
生产环境下，应通过后端服务生成短期有效凭证 / 令牌，而非将长期有效密钥打包至 APK 安装包内。

