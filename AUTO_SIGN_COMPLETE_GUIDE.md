# 🔐 完全自动化签名方案 - 使用指南

## 📋 方案说明

本方案实现了**完全自动化的 APK 签名和发布**，无需任何手动配置！

### ✨ 核心特点

- ✅ **零配置**: 直接使用仓库中的签名文件，无需配置 GitHub Secrets
- ✅ **自动签名**: Release APK 自动使用官方密钥签名
- ✅ **自动发布**: 打 Tag 时自动创建 GitHub Release
- ✅ **完整验证**: 自动验证 APK 签名有效性
- ✅ **清晰日志**: 每一步都有详细的状态输出

## 🚀 快速开始（2 分钟）

### 方法 1：通过网页创建（推荐）

1. **打开创建页面**:
   https://github.com/xiaohuai3344/DailyTask-master3344/new/master

2. **填写信息**:
   - 文件名: `.github/workflows/build-and-sign.yml`
   - 内容: 复制下面的完整配置

3. **提交**:
   - 提交信息: `feat: 添加完全自动化签名工作流`
   - 点击 "Commit new file"

### 方法 2：本地推送（需要处理权限）

```bash
cd /home/user/webapp
git add .github/workflows/build-and-sign.yml
git commit -m "feat: 添加完全自动化签名工作流"
git push origin master
```

## 📦 工作流程说明

### 触发条件

- ✅ Push 到 `master` 或 `genspark_ai_developer` 分支
- ✅ 打 Tag (如 `v2.2.5.1`)
- ✅ PR 到 `master` 分支
- ✅ 手动触发

### 构建步骤

1. **环境准备**
   - Checkout 代码
   - 设置 JDK 21
   - 授权 gradlew

2. **版本信息提取**
   - 从 `app/build.gradle` 提取版本信息
   - 生成提交哈希和构建日期

3. **验证签名文件**
   - 检查 `app/DailyTask.jks` 是否存在
   - 显示文件信息

4. **构建 APK**
   - 构建 Debug APK
   - 构建 Release APK（自动签名）

5. **验证签名**
   - 使用 `jarsigner` 验证
   - 使用 `apksigner` 验证
   - 确保签名有效

6. **组织输出**
   - 重命名 APK 文件
   - 复制到 `apk_output` 目录

7. **上传 Artifacts**
   - Debug APK (保留 30 天)
   - Release APK (保留 90 天)

8. **自动发布** (仅 Tag 触发)
   - 创建 Release Notes
   - 创建 GitHub Release
   - 附加 APK 文件

## 📥 下载和安装

### 从 Actions 下载

1. **访问 Actions 页面**:
   https://github.com/xiaohuai3344/DailyTask-master3344/actions

2. **找到最新成功的构建**

3. **下载 Artifacts**:
   - `DailyTask-debug-2.2.5.1-xxxxxx` - Debug 版本
   - `DailyTask-release-signed-2.2.5.1-xxxxxx` - 已签名的 Release 版本 ✅

### 从 Releases 下载（打 Tag 后）

1. **访问 Releases 页面**:
   https://github.com/xiaohuai3344/DailyTask-master3344/releases

2. **下载最新版本的 APK**:
   - `DailyTask_2.2.5.1_release_signed_xxxxxx.apk` - 推荐使用

### 安装步骤

1. **下载 APK** (推荐 Release 签名版)
2. **在手机上**:
   - 允许"安装未知应用"权限
   - 点击 APK 文件
   - 按提示完成安装

## 🔍 验证签名

### 在构建日志中查看

每次构建都会自动验证签名，查看步骤 "Verify APK Signature" 的输出。

### 手动验证（本地）

```bash
# 使用 apksigner 验证
$ANDROID_HOME/build-tools/*/apksigner verify --print-certs DailyTask_xxx.apk

# 使用 jarsigner 验证
jarsigner -verify -verbose -certs DailyTask_xxx.apk
```

### 预期输出

```
✅ APK is properly signed!
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
```

## 🏷️ 创建发布版本

### 方法 1：通过 Git 命令

```bash
# 创建并推送 Tag
git tag -a v2.2.5.1 -m "Release version 2.2.5.1"
git push origin v2.2.5.1
```

### 方法 2：通过 GitHub 网页

1. **访问 Releases 页面**:
   https://github.com/xiaohuai3344/DailyTask-master3344/releases/new

2. **填写信息**:
   - Tag: `v2.2.5.1`
   - Title: `DailyTask v2.2.5.1`
   - Description: 自动生成

3. **发布**:
   - 点击 "Publish release"
   - 等待 Actions 自动构建和上传 APK

## 📊 构建状态

### 查看构建进度

- **Actions 页面**: https://github.com/xiaohuai3344/DailyTask-master3344/actions
- **工作流详情**: 点击具体的运行记录
- **构建摘要**: 查看 "Build Summary" 部分

### 构建时间

- **首次构建**: 约 5-7 分钟
- **后续构建**: 约 3-5 分钟（有缓存）

## 🛠️ 故障排查

### 问题: APK 未签名或签名失败

**检查步骤**:
1. 确认 `app/DailyTask.jks` 文件存在
2. 查看工作流日志中的 "Verify Keystore" 步骤
3. 查看 "Verify APK Signature" 步骤的输出

### 问题: 无法下载 APK

**解决方案**:
1. 确认构建已成功完成（绿色勾号）
2. 在 Artifacts 区域查找对应的文件
3. 如果是 Tag 构建，也可以在 Releases 页面下载

### 问题: 安装时提示"未包含任何证书"

**原因**: APK 未正确签名

**解决方案**:
1. 下载 Release 版本而不是 Debug 版本
2. 确认下载的是 `*release_signed*` 文件
3. 查看构建日志确认签名步骤成功

## 📝 技术细节

### 签名配置

签名配置已在 `app/build.gradle` 中定义:

```gradle
signingConfigs {
    release {
        storeFile file('DailyTask.jks')
        storePassword '123456789'
        keyAlias 'key0'
        keyPassword '123456789'
    }
}
```

### APK 命名规则

- **Debug**: `DailyTask_{version}_debug_{date}.apk`
- **Release**: `DailyTask_{version}_release_signed_{date}.apk`

### 文件路径

- **Keystore**: `app/DailyTask.jks`
- **APK 输出**: `app/build/outputs/apk/daily/{debug|release}/`
- **最终输出**: `apk_output/`

## 🎯 最佳实践

### 开发阶段

- 使用 Debug APK 进行测试
- Push 到 `genspark_ai_developer` 分支自动构建

### 发布阶段

1. 合并到 `master` 分支
2. 创建 Tag (如 `v2.2.5.1`)
3. 等待自动构建和发布
4. 在 Releases 页面下载正式版 APK

### 版本管理

- 遵循语义化版本 (如 `v2.2.5.1`)
- 每个 Release 都包含完整的 Release Notes
- 保留历史版本供下载

## 📚 相关文档

- [完整交付报告](./COMPLETE_DELIVERY_REPORT.md)
- [功能使用说明](./WEEKEND_HOLIDAY_USAGE.md)
- [构建指南](./BUILD_GUIDE.md)

## ✅ 总结

本方案实现了:

- ✅ 零配置自动签名
- ✅ 自动构建和发布
- ✅ 完整的签名验证
- ✅ 清晰的状态输出
- ✅ 自动化 Release 创建

**无需任何手动操作，开箱即用！**

---

🔗 **快速链接**:
- [创建工作流](https://github.com/xiaohuai3344/DailyTask-master3344/new/master?filename=.github/workflows/build-and-sign.yml)
- [查看 Actions](https://github.com/xiaohuai3344/DailyTask-master3344/actions)
- [查看 Releases](https://github.com/xiaohuai3344/DailyTask-master3344/releases)
