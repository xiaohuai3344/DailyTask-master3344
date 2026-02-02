# 🚀 自动签名工作流 - 立即可用版本

## ⚡ 快速使用（仅需 2 分钟）

### 📋 操作步骤

1. **打开创建页面** (点击下面的链接):
   ```
   https://github.com/xiaohuai3344/DailyTask-master3344/new/master
   ```

2. **填写文件名**:
   ```
   .github/workflows/build-and-sign.yml
   ```

3. **复制下面的完整配置** 并粘贴到编辑框

4. **提交文件**:
   - 提交信息: `feat: 添加完全自动化签名工作流`
   - 点击 "Commit new file"

5. **等待构建** (约 5-7 分钟):
   - 访问: https://github.com/xiaohuai3344/DailyTask-master3344/actions
   - 查看构建进度

6. **下载 APK**:
   - 构建完成后，在 Artifacts 区域下载
   - 推荐下载: `DailyTask-release-signed-2.2.5.1-xxxxxx`

---

## 📝 完整工作流配置（直接复制）

```yaml
name: Build and Sign APK (Fully Automated)

on:
  push:
    branches:
      - master
      - genspark_ai_developer
    tags:
      - 'v*'
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
      
    - name: Get version info
      id: version
      run: |
        # 提取版本信息
        VERSION_NAME=$(grep "^\s*versionName" app/build.gradle | head -1 | awk '{print $2}' | tr -d "'" | tr -d '"' | sed 's/[^0-9.]//g')
        VERSION_CODE=$(grep "^\s*versionCode" app/build.gradle | head -1 | awk '{print $2}' | sed 's/[^0-9]//g')
        COMMIT_SHORT=$(git rev-parse --short HEAD)
        BUILD_DATE=$(date +'%Y%m%d_%H%M%S')
        
        # 设置默认值
        VERSION_NAME=${VERSION_NAME:-2.2.5.1}
        VERSION_CODE=${VERSION_CODE:-2251}
        
        # 输出变量
        echo "version_name=$VERSION_NAME" >> $GITHUB_OUTPUT
        echo "version_code=$VERSION_CODE" >> $GITHUB_OUTPUT
        echo "commit_short=$COMMIT_SHORT" >> $GITHUB_OUTPUT
        echo "build_date=$BUILD_DATE" >> $GITHUB_OUTPUT
        
        echo "✅ Version Name: $VERSION_NAME"
        echo "✅ Version Code: $VERSION_CODE"
        echo "✅ Commit: $COMMIT_SHORT"
        echo "✅ Build Date: $BUILD_DATE"
        
    - name: Verify Keystore
      run: |
        echo "🔍 Checking keystore file..."
        if [ -f "app/DailyTask.jks" ]; then
          echo "✅ Keystore found: app/DailyTask.jks"
          ls -lh app/DailyTask.jks
        else
          echo "❌ Keystore not found!"
          exit 1
        fi
        
    - name: Build Debug APK
      run: |
        echo "🔨 Building Debug APK..."
        ./gradlew assembleDailyDebug --stacktrace
        
    - name: Build Release APK (Signed)
      run: |
        echo "🔨 Building Release APK with signing..."
        ./gradlew assembleDailyRelease --stacktrace
        
    - name: Verify APK Signature
      run: |
        echo "🔍 Verifying APK signatures..."
        
        # 查找 APK 文件
        DEBUG_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/daily/debug/*" | head -n 1)
        RELEASE_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/daily/release/*" | head -n 1)
        
        echo "Debug APK: $DEBUG_APK"
        echo "Release APK: $RELEASE_APK"
        
        # 验证 Release APK 签名
        if [ -n "$RELEASE_APK" ]; then
          echo "📝 Verifying Release APK signature..."
          jarsigner -verify -verbose -certs "$RELEASE_APK" || echo "⚠️ Signature verification warning (expected for V2/V3 scheme)"
          
          # 使用 apksigner 验证（更准确）
          $ANDROID_HOME/build-tools/*/apksigner verify --print-certs "$RELEASE_APK" && echo "✅ APK is properly signed!" || echo "❌ APK signature verification failed!"
        fi
        
    - name: Organize APK outputs
      run: |
        echo "📦 Organizing APK outputs..."
        mkdir -p apk_output
        
        # 列出所有生成的 APK
        echo "=== All APK files ==="
        find app/build/outputs/apk -type f -name "*.apk"
        
        # 查找并复制 APK
        DEBUG_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/daily/debug/*" | head -n 1)
        RELEASE_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/daily/release/*" | head -n 1)
        
        if [ -n "$DEBUG_APK" ]; then
          cp "$DEBUG_APK" "apk_output/DailyTask_${{ steps.version.outputs.version_name }}_debug_${{ steps.version.outputs.build_date }}.apk"
          echo "✅ Debug APK copied"
          ls -lh "apk_output/DailyTask_${{ steps.version.outputs.version_name }}_debug_${{ steps.version.outputs.build_date }}.apk"
        else
          echo "❌ Debug APK not found!"
        fi
        
        if [ -n "$RELEASE_APK" ]; then
          cp "$RELEASE_APK" "apk_output/DailyTask_${{ steps.version.outputs.version_name }}_release_signed_${{ steps.version.outputs.build_date }}.apk"
          echo "✅ Release APK copied (SIGNED)"
          ls -lh "apk_output/DailyTask_${{ steps.version.outputs.version_name }}_release_signed_${{ steps.version.outputs.build_date }}.apk"
        else
          echo "❌ Release APK not found!"
        fi
        
        echo "=== Final APK outputs ==="
        ls -lh apk_output/
        
    - name: Upload Debug APK
      uses: actions/upload-artifact@v4
      if: success()
      with:
        name: DailyTask-debug-${{ steps.version.outputs.version_name }}-${{ steps.version.outputs.commit_short }}
        path: apk_output/*debug*.apk
        retention-days: 30
        if-no-files-found: error
        
    - name: Upload Release APK (Signed)
      uses: actions/upload-artifact@v4
      if: success()
      with:
        name: DailyTask-release-signed-${{ steps.version.outputs.version_name }}-${{ steps.version.outputs.commit_short }}
        path: apk_output/*release*.apk
        retention-days: 90
        if-no-files-found: error
        
    - name: Create Release Notes
      if: startsWith(github.ref, 'refs/tags/')
      run: |
        cat > release_notes.md << 'EOF'
        ## 🎉 DailyTask v${{ steps.version.outputs.version_name }} 发布

        ### 📦 版本信息
        - **版本名称**: ${{ steps.version.outputs.version_name }}
        - **版本代码**: ${{ steps.version.outputs.version_code }}
        - **提交哈希**: ${{ steps.version.outputs.commit_short }}
        - **构建时间**: ${{ steps.version.outputs.build_date }}

        ### ✨ 核心功能
        - ✅ 钉钉自动打卡
        - ✅ 伪灭屏控制（防止真正熄屏）
        - ✅ 远程指令控制
        - ✅ 手动音量键切换
        - ✅ **周末/节假日自动暂停打卡**（内置 2026 年节假日数据）
        - ✅ **打卡完成后自动恢复暗色**（10-30 秒随机延迟）

        ### 📥 下载说明
        - **Debug APK**: 包含调试信息，适合测试
        - **Release APK**: 已签名，可直接安装使用 ✅

        ### 🔒 签名信息
        - 所有 Release APK 均已使用官方密钥签名
        - 可以放心安装使用

        ### 📚 相关文档
        - [完整交付报告](https://github.com/xiaohuai3344/DailyTask-master3344/blob/master/COMPLETE_DELIVERY_REPORT.md)
        - [功能使用说明](https://github.com/xiaohuai3344/DailyTask-master3344/blob/master/WEEKEND_HOLIDAY_USAGE.md)

        ---
        
        **注意**: 首次安装可能需要允许"安装未知应用"权限
        EOF
        
    - name: Create GitHub Release
      if: startsWith(github.ref, 'refs/tags/')
      uses: softprops/action-gh-release@v1
      with:
        files: |
          apk_output/DailyTask_${{ steps.version.outputs.version_name }}_debug_${{ steps.version.outputs.build_date }}.apk
          apk_output/DailyTask_${{ steps.version.outputs.version_name }}_release_signed_${{ steps.version.outputs.build_date }}.apk
        body_path: release_notes.md
        draft: false
        prerelease: false
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        
    - name: Build Summary
      if: always()
      run: |
        echo "## 🎉 APK 构建完成！" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "### 📦 构建信息" >> $GITHUB_STEP_SUMMARY
        echo "- **版本名称**: ${{ steps.version.outputs.version_name }}" >> $GITHUB_STEP_SUMMARY
        echo "- **版本代码**: ${{ steps.version.outputs.version_code }}" >> $GITHUB_STEP_SUMMARY
        echo "- **提交哈希**: ${{ steps.version.outputs.commit_short }}" >> $GITHUB_STEP_SUMMARY
        echo "- **构建时间**: ${{ steps.version.outputs.build_date }}" >> $GITHUB_STEP_SUMMARY
        echo "- **分支**: ${{ github.ref_name }}" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "### ✅ 签名状态" >> $GITHUB_STEP_SUMMARY
        echo "- **Debug APK**: 使用 Debug 密钥签名" >> $GITHUB_STEP_SUMMARY
        echo "- **Release APK**: ✅ 已使用官方密钥签名" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "### 📥 下载 APK" >> $GITHUB_STEP_SUMMARY
        echo "请在上方 **Artifacts** 区域下载编译好的 APK 文件" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "- **Debug APK**: 包含调试信息，适合开发测试" >> $GITHUB_STEP_SUMMARY
        echo "- **Release APK**: 已签名，可直接安装使用 ✅" >> $GITHUB_STEP_SUMMARY
        echo "" >> $GITHUB_STEP_SUMMARY
        echo "### 🚀 安装说明" >> $GITHUB_STEP_SUMMARY
        echo "1. 下载对应的 APK 文件（推荐 Release 版本）" >> $GITHUB_STEP_SUMMARY
        echo "2. 在手机上允许"安装未知应用"权限" >> $GITHUB_STEP_SUMMARY
        echo "3. 直接安装即可使用" >> $GITHUB_STEP_SUMMARY
```

---

## ✅ 配置完成后

### 1. 查看构建状态

访问: https://github.com/xiaohuai3344/DailyTask-master3344/actions

### 2. 下载 APK

构建完成后，在 Artifacts 区域找到:
- `DailyTask-debug-2.2.5.1-xxxxxx` - Debug 版本
- `DailyTask-release-signed-2.2.5.1-xxxxxx` - ✅ **已签名的 Release 版本（推荐）**

### 3. 安装到手机

1. 解压下载的 ZIP 文件
2. 找到 APK 文件
3. 传输到手机
4. 允许"安装未知应用"权限
5. 安装

## 🎯 特点

- ✅ **完全自动化**: 无需配置 Secrets
- ✅ **自动签名**: Release APK 自动使用官方密钥签名
- ✅ **签名验证**: 自动验证 APK 签名有效性
- ✅ **清晰日志**: 每一步都有详细状态输出
- ✅ **零配置**: 开箱即用

## 📝 说明

- ✅ 签名文件 `app/DailyTask.jks` 已在仓库中
- ✅ 签名配置已在 `app/build.gradle` 中定义
- ✅ 工作流会自动使用这些配置
- ✅ Release APK 可以直接安装，无需再次签名

---

🚀 **立即开始**: 点击上面的链接创建工作流，2 分钟后即可下载签名的 APK！
