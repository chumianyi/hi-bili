#!/bin/bash
# Hi！bili 构建脚本 - 纯源码编译，零混淆零加密
# javac 直接编译 -> d8/dx 转 dex，类名方法名与源码完全一致
set -e
set -o pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
OUTPUT_DIR="$PROJECT_DIR/output"
APP_NAME="Hi！bili"

echo "============================================"
echo "  Hi！bili 构建 (零混淆 / 纯源码)"
echo "============================================"

# 检测可用 Android platform
ANDROID_PLATFORM=""
for p in 34 33 32 31 30 29 28 27 26 25 24 23 22 21 17; do
    if [ -f "$ANDROID_HOME/platforms/android-$p/android.jar" ]; then
        ANDROID_PLATFORM="$p"
        break
    fi
done
if [ -z "$ANDROID_PLATFORM" ]; then
    echo "ERROR: No Android platform found"
    exit 1
fi
echo "Using platform android-$ANDROID_PLATFORM"
ANDROID_JAR="$ANDROID_HOME/platforms/android-$ANDROID_PLATFORM/android.jar"
BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$OUTPUT_DIR" "$BUILD_DIR/classes" "$BUILD_DIR/gen" "$BUILD_DIR/compiled_res"

echo ""
echo "=== 1. 编译资源 (aapt2) ==="
find "$PROJECT_DIR/res" -type f \( -name "*.xml" -o -name "*.png" -o -name "*.jpg" \) -print0 | while IFS= read -r -d '' f; do
    "$BUILD_TOOLS/aapt2" compile "$f" -o "$BUILD_DIR/compiled_res/"
done

"$BUILD_TOOLS/aapt2" link \
    -I "$ANDROID_JAR" \
    --manifest "$PROJECT_DIR/AndroidManifest.xml" \
    -R "$BUILD_DIR/compiled_res/"*.flat \
    -o "$BUILD_DIR/app.apk" \
    --java "$BUILD_DIR/gen" \
    --auto-add-overlay

echo ""
echo "=== 2. 编译 Java (javac, 无混淆) ==="
# 收集所有源文件 - 确保不遗漏任何类
find "$PROJECT_DIR/src" -name "*.java" > "$BUILD_DIR/sources.txt"
find "$BUILD_DIR/gen" -name "*.java" >> "$BUILD_DIR/sources.txt"

echo "源文件列表:"
cat "$BUILD_DIR/sources.txt"

SRC_COUNT=$(wc -l < "$BUILD_DIR/sources.txt")
echo "共 $SRC_COUNT 个 Java 源文件"

CLASSPATH="$ANDROID_JAR"
if [ -f "$PROJECT_DIR/libs/core.jar" ]; then
    CLASSPATH="$CLASSPATH:$PROJECT_DIR/libs/core.jar"
fi

# 直接编译，不用管道吞错误 - 失败即退出
javac -source 1.7 -target 1.7 \
    -cp "$CLASSPATH" \
    -d "$BUILD_DIR/classes" \
    @"$BUILD_DIR/sources.txt"

echo "编译成功，class 文件:"
find "$BUILD_DIR/classes" -name "*.class" | sort

echo ""
echo "=== 3. DEX 转换 (d8/dx, 无混淆) ==="
if [ -f "$BUILD_TOOLS/d8" ]; then
    echo "Using d8"
    DEX_INPUTS=$(find "$BUILD_DIR/classes" -name "*.class")
    if [ -f "$PROJECT_DIR/libs/core.jar" ]; then
        "$BUILD_TOOLS/d8" --min-api 1 --output "$BUILD_DIR" $DEX_INPUTS "$PROJECT_DIR/libs/core.jar"
    else
        "$BUILD_TOOLS/d8" --min-api 1 --output "$BUILD_DIR" $DEX_INPUTS
    fi
else
    echo "Using dx"
    if [ -f "$PROJECT_DIR/libs/core.jar" ]; then
        "$BUILD_TOOLS/dx" --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes" "$PROJECT_DIR/libs/core.jar"
    else
        "$BUILD_TOOLS/dx" --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes"
    fi
fi

echo "DEX 生成成功"
ls -la "$BUILD_DIR/classes.dex"

echo ""
echo "=== 4. 打包 APK ==="
cd "$BUILD_DIR"
cp app.apk app_unsigned.apk
zip -j app_unsigned.apk classes.dex

echo ""
echo "=== 5. zipalign ==="
"$BUILD_TOOLS/zipalign" -f 4 app_unsigned.apk app_aligned.apk

echo ""
echo "=== 6. 签名 (v1+v2) ==="
if [ ! -f "$BUILD_DIR/debug.keystore" ]; then
    keytool -genkeypair -v -keystore "$BUILD_DIR/debug.keystore" \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Debug,O=Android,C=US"
fi

"$BUILD_TOOLS/apksigner" sign \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --ks "$BUILD_DIR/debug.keystore" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$OUTPUT_DIR/${APP_NAME}.apk" \
    app_aligned.apk

echo ""
echo "=== 验证签名 ==="
"$BUILD_TOOLS/apksigner" verify --verbose "$OUTPUT_DIR/${APP_NAME}.apk"

echo ""
echo "=== 构建完成 ==="
ls -la "$OUTPUT_DIR/"
