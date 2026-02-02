# 🚀 GitHub Actions 快速创建指南

由于 GitHub App 权限限制，无法直接推送工作流文件。请使用以下方式手动创建。

---

## ⚡ 方式一：一键快捷创建（推荐）

### 📝 直接复制工作流内容

点击下面的链接，会自动打开 GitHub 文件创建页面：

```
https://github.com/xiaohuai3344/DailyTask-master3344/new/genspark_ai_developer?filename=.github/workflows/build-apk.yml
```

**操作步骤**：

1. **打开链接** - 点击上面的 URL，会在 GitHub 打开文件创建页面
2. **粘贴内容** - 复制下面的完整 YAML 配置并粘贴
3. **提交文件** - 点击 "Commit new file" 按钮

### 📋 完整工作流配置

```yaml
name: Build Android APK

on:
  push:
    branches:
      - master
      - genspark_ai_developer
  pull_request:
    branches:
      - master
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug --stacktrace
      
    - name: Build Release APK
      run: ./gradlew assembleRelease --stacktrace
      
    - name: Get version info
      id: version
      run: |
        VERSION_NAME=$(grep "versionName" app/build.gradle | awk '{print $2}' | tr -d '"')
        VERSION_CODE=$(grep "versionCode" app/build.gradle | awk '{print $2}')
        COMMIT_SHORT=$(git rev-parse --short HEAD)
        BUILD_DATE=$(date +%Y%m%d_%H%M%S)
        echo "version_name=${VERSION_NAME}" >> $GITHUB_OUTPUT
        echo "version_code=${VERSION_CODE}" >> $GITHUB_OUTPUT
        echo "commit_short=${COMMIT_SHORT}" >> $GITHUB_OUTPUT
        echo "build_date=${BUILD_DATE}" >> $GITHUB_OUTPUT
        
    - name: Rename APK files
      run: |
        mv app/build/outputs/apk/debug/app-debug.apk \
           app/build/outputs/apk/debug/DailyTask_${{ steps.version.outputs.version_name }}_debug_${{ steps.version.outputs.build_date }}.apk
        if [ -f app/build/outputs/apk/release/app-release-unsigned.apk ]; then
          mv app/build/outputs/apk/release/app-release-unsigned.apk \
             app/build/outputs/apk/release/DailyTask_${{ steps.version.outputs.version_name }}_release_unsigned_${{ steps.version.outputs.build_date }}.apk
        fi
        
    - name: Upload Debug APK
      uses: actions/upload-artifact@v4
      with:
        name: DailyTask-debug-${{ steps.version.outputs.version_name }}-${{ steps.version.outputs.commit_short }}
        path: app/build/outputs/apk/debug/*.apk
        retention-days: 30
        
    - name: Upload Release APK
      uses: actions/upload-artifact@v4
      with:
        name: DailyTask-release-${{ steps.version.outputs.version_name }}-${{ steps.version.outputs.commit_short }}
        path: app/build/outputs/apk/release/*.apk
        retention-days: 90
        
    - name: Build Summary
      run: |
        echo "## 🎉 APK 编译成功！" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "### 📦 构建信息" >> $GITHUB_STEP_SUMMARY
        echo "- **版本名称**: ${{ steps.version.outputs.version_name }}" >> $GITHUB_STEP_SUMMARY
        echo "- **版本代码**: ${{ steps.version.outputs.version_code }}" >> $GITHUB_STEP_SUMMARY
        echo "- **提交哈希**: ${{ steps.version.outputs.commit_short }}" >> $GITHUB_STEP_SUMMARY
        echo "- **构建时间**: ${{ steps.version.outputs.build_date }}" >> $GITHUB_STEP_SUMMARY
        echo "- **分支**: ${{ github.ref_name }}" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "### 📥 下载 APK" >> $GITHUB_STEP_SUMMARY
        echo "请在上方 **Artifacts** 区域下载编译好的 APK 文件" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "- **Debug APK**: 用于开发调试（包含调试信息）" >> $GITHUB_STEP_SUMMARY
        echo "- **Release APK**: 用于生产发布（未签名，需要手动签名）" >> $GITHUB_STEP_SUMMARY
```

---

## 📱 方式二：通过 GitHub 网页操作

如果快捷链接不工作，按以下步骤手动创建：

### 步骤详解

1. **进入仓库**：
   ```
   https://github.com/xiaohuai3344/DailyTask-master3344
   ```

2. **切换分支**：
   - 点击左上角分支选择器（显示当前分支名）
   - 选择 `genspark_ai_developer` 分支

3. **创建新文件**：
   - 点击 "Add file" 按钮
   - 选择 "Create new file"

4. **设置文件路径**：
   - 在文件名输入框中输入：`.github/workflows/build-apk.yml`
   - 注意：输入 `/` 会自动创建目录

5. **粘贴配置**：
   - 将上面的完整 YAML 配置复制粘贴到编辑器

6. **提交文件**：
   - 滚动到页面底部
   - 填写提交信息（建议）：
     ```
     feat: 添加 GitHub Actions 自动编译 APK 功能
     ```
   - 选择 "Commit directly to the genspark_ai_developer branch"
   - 点击 "Commit new file" 按钮

7. **验证构建**：
   - 提交后会自动触发构建
   - 访问 Actions 页面查看：
     ```
     https://github.com/xiaohuai3344/DailyTask-master3344/actions
     ```

---

## 🎯 创建后验证

### 1️⃣ 检查工作流文件

访问以下链接确认文件已创建：
```
https://github.com/xiaohuai3344/DailyTask-master3344/blob/genspark_ai_developer/.github/workflows/build-apk.yml
```

### 2️⃣ 查看 Actions 页面

访问 Actions 页面：
```
https://github.com/xiaohuai3344/DailyTask-master3344/actions
```

应该能看到：
- ✅ "Build Android APK" 工作流已注册
- ✅ 如果刚提交文件，会看到正在运行的构建任务

### 3️⃣ 手动触发测试

如果想立即测试：

1. 在 Actions 页面点击 "Build Android APK" 工作流
2. 点击右侧 "Run workflow" 按钮
3. 选择 `genspark_ai_developer` 分支
4. 点击绿色的 "Run workflow" 按钮
5. 等待约 5-10 分钟
6. 构建完成后下载 APK

---

## 📥 下载编译好的 APK

构建成功后：

1. **进入 Actions 页面**：
   ```
   https://github.com/xiaohuai3344/DailyTask-master3344/actions
   ```

2. **选择最新的构建任务**（绿色勾号）

3. **滚动到 Artifacts 区域**，会看到：
   - `DailyTask-debug-{version}-{commit}` - Debug APK
   - `DailyTask-release-{version}-{commit}` - Release APK

4. **点击下载**，解压 ZIP 文件得到 APK

5. **安装到手机**测试

---

## ✨ 工作流功能说明

### 自动触发条件

- ✅ 推送代码到 `master` 或 `genspark_ai_developer` 分支
- ✅ 创建 PR 到 `master` 分支
- ✅ 在 Actions 页面手动触发

### 编译输出

- **Debug APK**: 调试版本，包含调试符号
- **Release APK**: 发布版本，经过优化（未签名）

### 文件命名规则

```
DailyTask_{版本号}_debug_{构建时间}.apk
DailyTask_{版本号}_release_unsigned_{构建时间}.apk
```

例如：
```
DailyTask_2.3.0.0_debug_20260202_141030.apk
DailyTask_2.3.0.0_release_unsigned_20260202_141030.apk
```

### 保留时间

- Debug APK: **30 天**
- Release APK: **90 天**

---

## 🔧 技术规格

- **运行环境**: Ubuntu latest
- **JDK 版本**: 21 (Temurin)
- **Gradle 版本**: 8.5（项目配置）
- **Kotlin 版本**: 2.0.21（项目配置）
- **构建时间**: 约 5-10 分钟
- **Gradle 缓存**: 启用（加快后续构建）

---

## ❓ 常见问题

### Q: 为什么需要手动创建？

**A**: GitHub App 需要 `workflows` 权限才能推送工作流文件。为了安全考虑，需要您手动创建。

### Q: 构建失败怎么办？

**A**: 
1. 点击失败的任务查看日志
2. 找到红色叉号的步骤
3. 查看错误信息
4. 如果是依赖问题，等待几分钟后重试
5. 如果是代码问题，需要修复代码

### Q: 如何修改工作流配置？

**A**:
1. 访问文件：https://github.com/xiaohuai3344/DailyTask-master3344/blob/genspark_ai_developer/.github/workflows/build-apk.yml
2. 点击右上角铅笔图标（Edit）
3. 修改配置
4. 提交更改

### Q: 可以在其他分支也自动编译吗？

**A**: 可以！修改工作流文件中的触发分支：

```yaml
on:
  push:
    branches:
      - master
      - genspark_ai_developer
      - develop  # 添加其他分支
```

---

## 📚 更多资源

- **详细配置指南**: 查看仓库中的 `GITHUB_ACTIONS_SETUP.md` 文件
- **GitHub Actions 文档**: https://docs.github.com/en/actions
- **问题反馈**: 在仓库中创建 Issue

---

## 🎉 快速链接汇总

### 🔗 一键创建工作流
```
https://github.com/xiaohuai3344/DailyTask-master3344/new/genspark_ai_developer?filename=.github/workflows/build-apk.yml
```

### 🔗 查看 Actions 构建状态
```
https://github.com/xiaohuai3344/DailyTask-master3344/actions
```

### 🔗 查看仓库文件
```
https://github.com/xiaohuai3344/DailyTask-master3344/tree/genspark_ai_developer
```

### 🔗 手动触发构建
```
https://github.com/xiaohuai3344/DailyTask-master3344/actions/workflows/build-apk.yml
```

---

**创建时间**: 2026年02月02日  
**文档版本**: 1.0  
**维护者**: GenSpark AI Developer

**祝您使用愉快！** 🚀
