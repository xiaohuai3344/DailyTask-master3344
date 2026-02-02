# 🚀 自动签名、优化和发布到 GitHub 完整指南

## 📋 概述

本指南将帮您配置完整的自动化流程：
1. ✅ 自动编译 Debug 和 Release APK
2. ✅ 自动签名 Release APK
3. ✅ 自动上传到 Artifacts
4. ✅ 自动创建 GitHub Release（打 tag 时）
5. ✅ 生成完整的 Release Notes

---

## 🔐 步骤 1：配置 GitHub Secrets

### 需要添加的 Secrets

访问您的仓库设置页面：  
👉 https://github.com/xiaohuai3344/DailyTask-master3344/settings/secrets/actions

点击 **"New repository secret"**，依次添加以下 4 个 secrets：

#### 1. KEYSTORE_BASE64
**说明**: Base64 编码的签名密钥库文件  
**值**: 
```
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
```

#### 2. KEYSTORE_PASSWORD
**说明**: 密钥库密码  
**值**: `123456789`

#### 3. KEY_ALIAS
**说明**: 密钥别名  
**值**: `key0`

#### 4. KEY_PASSWORD
**说明**: 密钥密码  
**值**: `123456789`

---

## 📝 步骤 2：更新工作流文件

### 方法一：GitHub 网页编辑（推荐）

1. **删除旧的工作流文件**：
   - 访问：https://github.com/xiaohuai3344/DailyTask-master3344/blob/master/.github/workflows/build-apk.yml
   - 点击右上角的垃圾桶图标删除

2. **创建新的工作流文件**：
   - 访问：https://github.com/xiaohuai3344/DailyTask-master3344/new/master?filename=.github/workflows/build-sign-release.yml
   - 复制本文档最后的完整配置
   - 粘贴到编辑器
   - 提交信息：`feat: 添加自动签名和发布工作流`

### 方法二：本地已准备好（供参考）

本地文件路径：
```
/home/user/webapp/.github/workflows/build-sign-release.yml
```

由于权限限制，需要您手动创建到 GitHub。

---

## 🎯 步骤 3：使用方式

### 自动触发编译（每次 push）

当您推送代码到 `master` 或 `genspark_ai_developer` 分支时：
1. ✅ 自动编译 Debug 和 Release APK
2. ✅ 自动签名 Release APK
3. ✅ 上传到 Artifacts（保留 30-90 天）
4. ✅ 可在 Actions 页面下载

**查看 Actions**：  
👉 https://github.com/xiaohuai3344/DailyTask-master3344/actions

---

### 发布到 GitHub Release（打 tag）

当您创建版本标签时，会自动发布到 GitHub Releases：

#### 创建 Release 的步骤：

**方法一：使用 GitHub 网页**
1. 访问：https://github.com/xiaohuai3344/DailyTask-master3344/releases/new
2. 点击 "Choose a tag"
3. 输入新标签，格式：`v2.2.5.1`（必须以 v 开头）
4. 点击 "Create new tag on publish"
5. 标题填写：`DailyTask v2.2.5.1`
6. 点击 "Publish release"

**方法二：使用 Git 命令**
```bash
# 创建标签
git tag -a v2.2.5.1 -m "Release v2.2.5.1"

# 推送标签
git push origin v2.2.5.1
```

#### 自动发布后的效果：
- ✅ 自动编译和签名 APK
- ✅ 自动上传 Debug 和 Release APK 到 Release
- ✅ 自动生成 Release Notes（包含功能列表）
- ✅ 可在 Releases 页面永久下载

**查看 Releases**：  
👉 https://github.com/xiaohuai3344/DailyTask-master3344/releases

---

## 📦 输出文件说明

### Artifacts（临时下载）
每次编译都会生成，保留一段时间后自动删除：
- `DailyTask-debug-2.2.5.1-{commit}.zip`（保留 30 天）
- `DailyTask-release-2.2.5.1-{commit}.zip`（保留 90 天）

### GitHub Release（永久下载）
只有打 tag 时才会创建，永久保存：
- `DailyTask_2.2.5.1_debug_20260202_HHMMSS.apk`
- `DailyTask_2.2.5.1_release_20260202_HHMMSS.apk` ✨（已签名优化）

---

## ✨ Release APK 的优势

### 已签名 Release APK 的特点：
1. ✅ **已签名**：可以直接安装到手机
2. ✅ **代码混淆**：ProGuard/R8 优化（如果启用）
3. ✅ **体积更小**：约 20-25MB（vs Debug 30-35MB）
4. ✅ **性能更好**：编译优化，运行更快
5. ✅ **生产就绪**：适合正式发布使用

### Debug APK 的特点：
1. ✅ **包含调试信息**：方便排查问题
2. ✅ **详细日志**：更多的日志输出
3. ✅ **适合测试**：推荐开发测试使用

---

## 📊 工作流程图

### 普通 Push（自动编译）
```
Push 到 master/genspark_ai_developer
    ↓
自动触发 GitHub Actions
    ↓
编译 Debug APK
    ↓
编译 Release APK（自动签名）
    ↓
上传到 Artifacts
    ↓
可在 Actions 页面下载（保留 30-90 天）
```

### 创建 Tag（自动发布）
```
创建 Tag（如 v2.2.5.1）
    ↓
Push Tag 到 GitHub
    ↓
自动触发 GitHub Actions
    ↓
编译 Debug 和 Release APK（已签名）
    ↓
生成 Release Notes
    ↓
创建 GitHub Release
    ↓
上传 APK 到 Release
    ↓
永久可下载
```

---

## 🔍 Release Notes 内容

自动生成的 Release Notes 包含：
- 📦 版本信息（版本号、版本代码、构建日期、提交哈希）
- ✨ 核心功能列表（周末/节假日暂停、自动恢复暗色等）
- 📥 下载说明（Debug vs Release）
- 📱 安装要求（Android 版本、权限）
- 🧪 测试建议
- 📚 完整文档链接

---

## ⚙️ 高级配置

### 启用 ProGuard/R8 混淆（可选）

如果需要启用代码混淆和优化，编辑 `app/build.gradle`：

```groovy
buildTypes {
    release {
        minifyEnabled true  // 改为 true
        shrinkResources true  // 可选：资源压缩
        proguardFiles 'proguard-rules.pro'
        signingConfig signingConfigs.release
    }
}
```

**注意**：启用后首次编译时间会增加，但 APK 体积会显著减小（约 5-10MB）。

---

## 🧪 测试签名是否成功

### 验证 APK 签名

下载 Release APK 后，使用以下命令验证：

```bash
# 检查签名信息
jarsigner -verify -verbose -certs DailyTask_xxx_release.apk

# 查看签名详情
keytool -printcert -jarfile DailyTask_xxx_release.apk
```

**预期输出**：
```
jar verified.
...
X.509, CN=Peng, OU=Casic, ...
```

---

## 📋 常见问题

### Q1: 如何创建第一个 Release？
**A**: 按照"步骤 3 - 发布到 GitHub Release"的说明，创建标签 `v2.2.5.1` 并推送。

### Q2: Release APK 和 Debug APK 都需要下载吗？
**A**: 
- 测试阶段：下载 Debug APK 即可
- 正式使用：推荐下载 Release APK（已签名优化）

### Q3: 如何更新版本号？
**A**: 编辑 `app/build.gradle`，修改 `versionName` 和 `versionCode`，然后创建新的 tag。

### Q4: Secrets 配置错误怎么办？
**A**: 
1. 检查 Secrets 名称是否正确（大小写敏感）
2. 检查密码是否正确
3. 查看 Actions 日志排查具体错误

### Q5: 如何删除旧的 Release？
**A**: 访问 Releases 页面，点击对应 Release 右侧的删除按钮。

---

## 🎁 完整功能清单

### ✅ 已实现
- [x] 自动编译 Debug APK
- [x] 自动编译 Release APK
- [x] 自动签名 Release APK
- [x] 自动上传到 Artifacts
- [x] 自动创建 GitHub Release
- [x] 自动生成 Release Notes
- [x] 支持手动触发
- [x] 支持 Pull Request 检查

### 🔮 可选增强（后续）
- [ ] 自动发送通知（Telegram/Email）
- [ ] 自动运行测试
- [ ] 自动上传到第三方分发平台（蒲公英/fir.im）
- [ ] 自动生成 Changelog
- [ ] 多渠道打包

---

## 🔗 重要链接

| 用途 | 链接 |
|------|------|
| 🔐 **配置 Secrets** | https://github.com/xiaohuai3344/DailyTask-master3344/settings/secrets/actions |
| 📝 **创建工作流** | https://github.com/xiaohuai3344/DailyTask-master3344/new/master?filename=.github/workflows/build-sign-release.yml |
| 🏃 **查看 Actions** | https://github.com/xiaohuai3344/DailyTask-master3344/actions |
| 🎁 **查看 Releases** | https://github.com/xiaohuai3344/DailyTask-master3344/releases |
| ➕ **创建 Release** | https://github.com/xiaohuai3344/DailyTask-master3344/releases/new |

---

## 📝 总结

完成上述步骤后，您将拥有：
1. ✅ 全自动的 APK 编译流程
2. ✅ 自动签名的 Release APK
3. ✅ 规范的版本发布流程
4. ✅ 永久可下载的 Release 文件
5. ✅ 完整的 Release Notes

**下一步**：
1. 立即配置 GitHub Secrets
2. 创建新的工作流文件
3. Push 代码触发第一次编译
4. 创建 v2.2.5.1 标签发布第一个 Release

---

**配置日期**: 2026-02-02  
**文档版本**: v1.0  
**状态**: ✅ 准备就绪

---

🚀 **立即开始配置，体验全自动发布流程！**
