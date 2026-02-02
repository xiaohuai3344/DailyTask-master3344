# 🔧 GitHub Actions APK 上传问题修复（第四次修复）

## ⚠️ 发现的问题

编译成功，但没有找到 APK 文件上传：

```
Warning: No files were found with the provided path: apk_output/*debug*.apk
Warning: No files were found with the provided path: apk_output/*release*.apk
```

## 🔍 根本原因

**问题分析**：
- 项目使用了 **Product Flavors**（`daily` flavor）
- APK 实际输出路径是：
  - `app/build/outputs/apk/daily/debug/DT_20260202_2.2.5.1.apk`
  - `app/build/outputs/apk/daily/release/DT_20260202_2.2.5.1.apk`
  
- 而不是原来假设的：
  - `app/build/outputs/apk/debug/`
  - `app/build/outputs/apk/release/`

**Product Flavors 配置**（在 `app/build.gradle` 中）：
```groovy
productFlavors {
    daily {
        applicationId = "com.alibaba.android.${createRandomCode()}"
    }
}
```

---

## ✅ 最终修复方案

使用 `find` 命令递归搜索整个 `app/build/outputs/apk` 目录，自动找到所有 Debug 和 Release APK。

---

## 📋 完整的最终修复版配置（第四版）

**请完整复制以下内容，替换整个工作流文件：**

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
        # 提取版本信息（更精确的匹配，避免匹配到其他行）
        VERSION_NAME=$(grep "^\s*versionName" app/build.gradle | head -1 | awk '{print $2}' | tr -d "'" | tr -d '"')
        VERSION_CODE=$(grep "^\s*versionCode" app/build.gradle | head -1 | awk '{print $2}')
        COMMIT_SHORT=$(git rev-parse --short HEAD)
        BUILD_DATE=$(date +'%Y%m%d_%H%M%S')
        
        # 验证提取的值不为空且格式正确
        if [ -z "$VERSION_NAME" ] || [[ "$VERSION_NAME" == *"+"* ]] || [[ "$VERSION_NAME" == *"="* ]]; then
          VERSION_NAME="2.2.5.1"
          echo "Warning: Could not extract versionName correctly, using default: $VERSION_NAME"
        fi
        if [ -z "$VERSION_CODE" ] || [[ "$VERSION_CODE" == *"+"* ]] || [[ "$VERSION_CODE" == *"="* ]]; then
          VERSION_CODE="2251"
          echo "Warning: Could not extract versionCode correctly, using default: $VERSION_CODE"
        fi
        
        # 清理可能的特殊字符
        VERSION_NAME=$(echo "$VERSION_NAME" | sed 's/[^0-9.]//g')
        VERSION_CODE=$(echo "$VERSION_CODE" | sed 's/[^0-9]//g')
        
        # 打印调试信息
        echo "Extracted Version Name: $VERSION_NAME"
        echo "Extracted Version Code: $VERSION_CODE"
        echo "Commit: $COMMIT_SHORT"
        echo "Build Date: $BUILD_DATE"
        
        # 输出到 GitHub Actions（确保格式正确）
        echo "version_name=$VERSION_NAME" >> "$GITHUB_OUTPUT"
        echo "version_code=$VERSION_CODE" >> "$GITHUB_OUTPUT"
        echo "commit_short=$COMMIT_SHORT" >> "$GITHUB_OUTPUT"
        echo "build_date=$BUILD_DATE" >> "$GITHUB_OUTPUT"
        
    - name: Rename APK files
      run: |
        # 列出 APK 输出目录结构
        echo "=== APK Output Directory Structure ==="
        find app/build/outputs/apk -type f -name "*.apk" || echo "No APK files found"
        
        # 创建输出目录
        mkdir -p apk_output
        
        # 查找并复制 Debug APK（支持所有可能的路径）
        DEBUG_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/debug/*" | head -n 1)
        if [ -n "$DEBUG_APK" ]; then
          cp "$DEBUG_APK" "apk_output/DailyTask_${{ steps.version.outputs.version_name }}_debug_${{ steps.version.outputs.build_date }}.apk"
          echo "✓ Debug APK copied: $DEBUG_APK"
        else
          echo "✗ Warning: Debug APK not found"
        fi
        
        # 查找并复制 Release APK（支持所有可能的路径）
        RELEASE_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/release/*" | head -n 1)
        if [ -n "$RELEASE_APK" ]; then
          cp "$RELEASE_APK" "apk_output/DailyTask_${{ steps.version.outputs.version_name }}_release_unsigned_${{ steps.version.outputs.build_date }}.apk"
          echo "✓ Release APK copied: $RELEASE_APK"
        else
          echo "✗ Warning: Release APK not found"
        fi
        
        # 显示最终输出
        echo "=== Final APK files ==="
        ls -lh apk_output/ || echo "No files in apk_output"
        
    - name: Upload Debug APK
      uses: actions/upload-artifact@v4
      if: success()
      with:
        name: DailyTask-debug-${{ steps.version.outputs.version_name }}-${{ steps.version.outputs.commit_short }}
        path: apk_output/*debug*.apk
        retention-days: 30
        if-no-files-found: warn
        
    - name: Upload Release APK
      uses: actions/upload-artifact@v4
      if: success()
      with:
        name: DailyTask-release-${{ steps.version.outputs.version_name }}-${{ steps.version.outputs.commit_short }}
        path: apk_output/*release*.apk
        retention-days: 90
        if-no-files-found: warn
        
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

## 🔍 关键修复点

### 修复前（错误的路径）：
```bash
DEBUG_APK=$(find app/build/outputs/apk/debug -name "*.apk" -type f | head -n 1)
RELEASE_APK=$(find app/build/outputs/apk/release -name "*.apk" -type f | head -n 1)
```

### 修复后（递归搜索所有路径）：
```bash
# 从 apk 根目录递归搜索，匹配路径包含 /debug/ 的 APK
DEBUG_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/debug/*" | head -n 1)

# 从 apk 根目录递归搜索，匹配路径包含 /release/ 的 APK
RELEASE_APK=$(find app/build/outputs/apk -type f -name "*.apk" -path "*/release/*" | head -n 1)
```

**工作原理**：
- 搜索整个 `app/build/outputs/apk` 目录
- 匹配所有 `.apk` 文件
- 使用 `-path` 过滤路径包含 `/debug/` 或 `/release/` 的文件
- 自动适配各种 flavor 和 build variant

---

## 📊 APK 路径示例

### 标准项目（无 flavors）：
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

### 带 Product Flavors 的项目（本项目）：
```
app/build/outputs/apk/daily/debug/DT_20260202_2.2.5.1.apk
app/build/outputs/apk/daily/release/DT_20260202_2.2.5.1.apk
```

### 多个 Flavors 的项目：
```
app/build/outputs/apk/daily/debug/app-daily-debug.apk
app/build/outputs/apk/prod/debug/app-prod-debug.apk
app/build/outputs/apk/daily/release/app-daily-release.apk
app/build/outputs/apk/prod/release/app-prod-release.apk
```

**新的 find 命令可以自动适配所有这些情况！**

---

## 🎯 使用步骤

1. **打开编辑页面**：  
   https://github.com/xiaohuai3344/DailyTask-master3344/edit/master/.github/workflows/build-apk.yml

2. **删除所有内容**

3. **复制上面的完整配置**

4. **粘贴到编辑器**

5. **提交信息**：
   ```
   fix: 修复 APK 路径问题以支持 Product Flavors
   ```

6. **提交并等待编译**

---

## ✅ 预期结果

### 编译日志应该显示：
```
=== APK Output Directory Structure ===
app/build/outputs/apk/daily/debug/DT_20260202_2.2.5.1.apk
app/build/outputs/apk/daily/release/DT_20260202_2.2.5.1.apk

✓ Debug APK copied: app/build/outputs/apk/daily/debug/DT_20260202_2.2.5.1.apk
✓ Release APK copied: app/build/outputs/apk/daily/release/DT_20260202_2.2.5.1.apk

=== Final APK files ===
-rw-r--r-- 1 runner docker 31M Feb 2 08:15 DailyTask_2.2.5.1_debug_20260202_081530.apk
-rw-r--r-- 1 runner docker 21M Feb 2 08:15 DailyTask_2.2.5.1_release_unsigned_20260202_081530.apk
```

### Artifacts 上传成功：
```
✓ Upload Debug APK
✓ Upload Release APK
```

---

## 📝 修复历程总结

### 四次修复历程：
1. **第一次**：处理单引号/双引号问题 ✅
2. **第二次**：改进文件处理和调试信息 ✅
3. **第三次**：修复 grep 匹配问题（避免匹配 flavorDimensions）✅
4. **第四次**（本次）：支持 Product Flavors 的 APK 路径 ✅

### 最终方案：
- ✅ 精确的版本信息提取
- ✅ 递归搜索支持所有 APK 路径结构
- ✅ 自动适配 Product Flavors
- ✅ 详细的调试信息
- ✅ 完善的错误处理

---

**修复日期**：2026-02-02  
**修复版本**：v4（最终版）  
**状态**：✅ 准备就绪

---

🚀 **请立即使用上面的配置更新工作流文件！这次应该可以正确上传 APK 了！**
