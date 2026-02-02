# 🚀 快速配置：5 分钟完成自动签名和发布

## 📋 配置清单

### ✅ 第 1 步：添加 GitHub Secrets（3 分钟）

访问：https://github.com/xiaohuai3344/DailyTask-master3344/settings/secrets/actions

点击 **"New repository secret"**，依次添加 4 个 secrets：

#### Secret 1: KEYSTORE_BASE64
```
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
```

#### Secret 2: KEYSTORE_PASSWORD
```
123456789
```

#### Secret 3: KEY_ALIAS
```
key0
```

#### Secret 4: KEY_PASSWORD
```
123456789
```

---

### ✅ 第 2 步：创建工作流文件（2 分钟）

由于权限限制，工作流文件已在本地准备好，需要您手动创建：

**查看完整配置文件**：
📄 `/home/user/webapp/.github/workflows/build-sign-release.yml`

**或查看详细文档**：
📄 `AUTO_SIGN_RELEASE_GUIDE.md`

---

## 🎯 完成后的效果

### 每次 Push 到 master
✅ 自动编译 Debug APK（未签名）  
✅ 自动编译 Release APK（**已签名** ✨）  
✅ 上传到 Artifacts（可下载 30-90 天）

### 创建 Tag 时（如 v2.2.5.1）
✅ 自动编译两个版本  
✅ 自动生成 Release Notes  
✅ 自动创建 GitHub Release  
✅ APK **永久可下载** 🎁

---

## 📥 下载 APK 的方式

### 方式 1：从 Artifacts 下载（临时）
👉 https://github.com/xiaohuai3344/DailyTask-master3344/actions

- 适合：日常测试
- 保留：30-90 天
- 文件：Debug 和 Release（已签名）

### 方式 2：从 Releases 下载（永久）
👉 https://github.com/xiaohuai3344/DailyTask-master3344/releases

- 适合：正式发布
- 保留：永久
- 文件：Debug 和 Release（已签名）
- 附带：完整的 Release Notes

---

## 🎁 Release APK 的优势

与之前的未签名版本相比：

| 特性 | 之前（未签名） | 现在（已签名）✨ |
|------|----------------|-----------------|
| 可直接安装 | ❌ 否 | ✅ 是 |
| 需要手动签名 | ✅ 是 | ❌ 否 |
| 体积 | ~20MB | ~20MB |
| 性能 | 优化 | 优化 |
| 推荐使用 | - | ✅ 生产环境 |

---

## 🚀 如何创建第一个 Release

### 方法一：GitHub 网页（推荐）
1. 访问：https://github.com/xiaohuai3344/DailyTask-master3344/releases/new
2. 输入 Tag：`v2.2.5.1`（必须以 v 开头）
3. 标题：`DailyTask v2.2.5.1`
4. 点击 "Publish release"
5. 等待 5-7 分钟，自动编译完成
6. 刷新页面，下载 APK

### 方法二：Git 命令
```bash
git tag -a v2.2.5.1 -m "Release v2.2.5.1"
git push origin v2.2.5.1
```

---

## 📊 自动生成的 Release Notes 内容

- 📦 版本信息（版本号、构建日期、提交哈希）
- ✨ 核心功能列表
  - 钉钉自动打卡
  - 周末/节假日自动暂停
  - 打卡完成自动恢复暗色
  - 等等...
- 📥 下载说明（Debug vs Release）
- 📱 安装要求
- 🧪 测试建议
- 📚 完整文档链接

---

## 🔗 快速链接

| 操作 | 链接 |
|------|------|
| 🔐 **配置 Secrets** | https://github.com/xiaohuai3344/DailyTask-master3344/settings/secrets/actions |
| 🏃 **查看 Actions** | https://github.com/xiaohuai3344/DailyTask-master3344/actions |
| 🎁 **查看 Releases** | https://github.com/xiaohuai3344/DailyTask-master3344/releases |
| ➕ **创建 Release** | https://github.com/xiaohuai3344/DailyTask-master3344/releases/new |

---

## ✅ 配置检查清单

- [ ] 已添加 `KEYSTORE_BASE64` Secret
- [ ] 已添加 `KEYSTORE_PASSWORD` Secret
- [ ] 已添加 `KEY_ALIAS` Secret  
- [ ] 已添加 `KEY_PASSWORD` Secret
- [ ] 已创建工作流文件 `build-sign-release.yml`
- [ ] 已测试 Push 触发编译
- [ ] 已创建第一个 Tag/Release

---

**准备好了吗？立即开始配置！** 🚀

完整文档请查看：`AUTO_SIGN_RELEASE_GUIDE.md`
